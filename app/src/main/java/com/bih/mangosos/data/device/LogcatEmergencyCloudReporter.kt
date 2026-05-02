package com.bih.mangosos.data.device

import android.util.Log
import com.bih.mangosos.domain.EmergencyCloudReport
import com.bih.mangosos.domain.EmergencyCloudReporter

class LogcatEmergencyCloudReporter : EmergencyCloudReporter {
    override fun report(report: EmergencyCloudReport) {
        Log.d(
            "MangoEmergencyCloud",
            "Emergency report fallback: " +
                "protectedPerson=${report.protectedPersonName}, " +
                "smsContact=${report.emergencyContactName}, " +
                "whatsappContact=${report.whatsappContactName}, " +
                "contacts=${report.emergencyContacts.size}, " +
                "whatsapp=${report.whatsappContact.isNotBlank()}, " +
                "location=${report.location != null}, " +
                "photo=${report.photoFile != null}, " +
                "video=${report.videoFile != null}, " +
                "callStatus=${report.callStatus}, " +
                "locationShareStatus=${report.locationShareStatus}",
        )
    }
}
