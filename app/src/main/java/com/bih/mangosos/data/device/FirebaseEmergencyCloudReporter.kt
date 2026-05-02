package com.bih.mangosos.data.device

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bih.mangosos.domain.EmergencyCloudReport
import com.bih.mangosos.domain.EmergencyCloudReporter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class FirebaseEmergencyCloudReporter(
    context: Context,
    private val fallback: EmergencyCloudReporter,
) : EmergencyCloudReporter {
    private val appContext = context.applicationContext

    override fun report(report: EmergencyCloudReport) {
        val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrElse {
            fallback.report(report)
            return
        }
        val document = firestore.collection(COLLECTION).document()
        val baseData = report.toFirestoreMap(document.id)

        val storage = runCatching { FirebaseStorage.getInstance().reference }.getOrElse {
            saveReport(
                document.id,
                firestore,
                baseData + mapOf(
                    "photoUploadStatus" to if (report.photoFile != null) "storage_unavailable" else "not_captured",
                    "videoUploadStatus" to if (report.videoFile != null) "storage_unavailable" else "not_captured",
                ),
            )
            fallback.report(report)
            return
        }

        val photoFile = report.photoFile?.takeIf { it.exists() }
        val videoFile = report.videoFile?.takeIf { it.exists() }

        if (photoFile == null && videoFile == null) {
            saveReport(document.id, firestore, baseData)
            return
        }

        saveReport(document.id, firestore, baseData + mediaStatus(photoFile, videoFile))

        photoFile?.let { file ->
            uploadMedia(
                firestore = firestore,
                reportId = document.id,
                file = file,
                storagePath = "emergency_reports/${document.id}/sos_photo.jpg",
                statusField = "photoUploadStatus",
                pathField = "photoStoragePath",
                urlField = "photoDownloadUrl",
            )
        }

        videoFile?.let { file ->
            uploadMedia(
                firestore = firestore,
                reportId = document.id,
                file = file,
                storagePath = "emergency_reports/${document.id}/sos_video.mp4",
                statusField = "videoUploadStatus",
                pathField = "videoStoragePath",
                urlField = "videoDownloadUrl",
            )
        }
    }

    private fun uploadMedia(
        firestore: FirebaseFirestore,
        reportId: String,
        file: java.io.File,
        storagePath: String,
        statusField: String,
        pathField: String,
        urlField: String,
    ) {
        val mediaRef = FirebaseStorage.getInstance().reference.child(storagePath)
        mediaRef.putFile(Uri.fromFile(file))
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: IllegalStateException("Media upload failed")
                }
                mediaRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                firestore.collection(COLLECTION).document(reportId).update(
                    mapOf(
                        statusField to "uploaded",
                        pathField to storagePath,
                        urlField to downloadUri.toString(),
                        "updatedAtServer" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            .addOnFailureListener { error ->
                Log.e("MangoEmergencyCloud", "Failed to upload emergency media", error)
                firestore.collection(COLLECTION).document(reportId).update(
                    mapOf(
                        statusField to "failed",
                        "${statusField}Error" to error.javaClass.simpleName,
                        "updatedAtServer" to FieldValue.serverTimestamp(),
                    ),
                )
            }
    }

    private fun saveReport(
        reportId: String,
        firestore: FirebaseFirestore,
        data: Map<String, Any?>,
    ) {
        firestore.collection(COLLECTION)
            .document(reportId)
            .set(data)
            .addOnFailureListener { error ->
                Log.e("MangoEmergencyCloud", "Failed to save emergency report", error)
            }
    }

    private fun EmergencyCloudReport.toFirestoreMap(reportId: String): Map<String, Any?> {
        val latitude = location?.latitude
        val longitude = location?.longitude
        return mapOf(
            "reportId" to reportId,
            "appPackage" to appContext.packageName,
            "protectedPersonName" to protectedPersonName,
            "emergencyContacts" to emergencyContacts,
            "emergencyContactName" to emergencyContactName,
            "whatsappContact" to whatsappContact,
            "whatsappContactName" to whatsappContactName,
            "triggerSource" to triggerSource,
            "callStatus" to callStatus,
            "locationShareStatus" to locationShareStatus,
            "smsAttempted" to emergencyContacts.isNotEmpty(),
            "whatsappAttempted" to whatsappContact.isNotBlank(),
            "hasPhoto" to (photoFile != null),
            "hasVideo" to (videoFile != null),
            "hasLocation" to (location != null),
            "latitude" to latitude,
            "longitude" to longitude,
            "mapUrl" to if (latitude != null && longitude != null) {
                "https://maps.google.com/?q=$latitude,$longitude"
            } else {
                null
            },
            "createdAtDevice" to Timestamp.now(),
            "createdAtServer" to FieldValue.serverTimestamp(),
            "schemaVersion" to 1,
        )
    }

    private fun mediaStatus(
        photoFile: java.io.File?,
        videoFile: java.io.File?,
    ): Map<String, String> {
        return mapOf(
            "photoUploadStatus" to if (photoFile != null) "uploading" else "not_captured",
            "videoUploadStatus" to if (videoFile != null) "uploading" else "not_captured",
        )
    }

    private companion object {
        const val COLLECTION = "emergency_reports"
    }
}
