package com.bih.mangosos.data.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bih.mangosos.MainActivity
import com.bih.mangosos.domain.SosLocation
import com.bih.mangosos.domain.SosPlatformActions
import com.bih.mangosos.service.SosForegroundService
import java.io.File

class AndroidSosPlatformActions(
    private val context: Context,
) : SosPlatformActions {
    override fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    override fun startForegroundSync() {
        val intent = Intent(context, SosForegroundService::class.java).apply {
            action = SosForegroundService.ACTION_SYNC_NOTIFICATION
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun launchMainActivity() {
        val intent = MainActivity.createLaunchIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    override fun sendWhatsAppAlert(
        whatsappNumber: String,
        protectedPersonName: String,
        languageCode: String,
        photoFile: File?,
        lastLocation: SosLocation?,
    ) {
        val pkg = when {
            isAppInstalled("com.whatsapp") -> "com.whatsapp"
            isAppInstalled("com.whatsapp.w4b") -> "com.whatsapp.w4b"
            else -> return
        }

        val message = context.buildWhatsAppEmergencyMessage(
            protectedPersonName = protectedPersonName,
            languageCode = languageCode,
            location = lastLocation,
        )

        val digits = normalizeWhatsAppDigits(whatsappNumber)

        val intent = if (photoFile != null && photoFile.exists()) {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, message)
                addWhatsAppContactExtras(digits)
                setPackage(pkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
            Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(pkg)
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AndroidSosPlatform", "Failed to launch WhatsApp", e)
        }
    }

    private fun Intent.addWhatsAppContactExtras(digits: String) {
        val jid = "$digits@s.whatsapp.net"
        putExtra("jid", jid)
        putExtra("com.whatsapp.EXTRA_JID", jid)
        putExtra("com.whatsapp.contact.ID", jid)
        putExtra("address", digits)
    }

    private fun normalizeWhatsAppDigits(whatsappNumber: String): String {
        var digits = whatsappNumber.filter { it.isDigit() }
        if (digits.startsWith("0") && digits.length == 10) {
            digits = "972" + digits.substring(1)
        } else if (digits.length == 9 && digits.startsWith("5")) {
            digits = "972" + digits
        } else if (digits.length == 10 && !digits.startsWith("972")) {
            digits = "972" + digits
        }
        return digits
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: Exception) {
            false
        }
    }
}
