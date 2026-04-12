package com.example.sosapp.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.sosapp.data.CallStatus

class CallHandler(
    private val context: Context,
) {
    fun placeDirectCall(number: String): CallStatus {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runIntent(intent, CallStatus.PLACED_DIRECT_CALL)
            ?: openDialerFallback(number)
    }

    fun openDialerFallback(number: String): CallStatus {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runIntent(intent, CallStatus.OPENED_DIALER_FALLBACK) ?: CallStatus.FAILED
    }

    private fun runIntent(intent: Intent, successStatus: CallStatus): CallStatus? {
        return try {
            context.startActivity(intent)
            successStatus
        } catch (_: SecurityException) {
            null
        } catch (_: ActivityNotFoundException) {
            null
        }
    }
}
