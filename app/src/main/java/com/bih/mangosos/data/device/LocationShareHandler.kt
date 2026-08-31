package com.bih.mangosos.data.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bih.mangosos.R
import com.bih.mangosos.data.LocationShareStatus
import com.bih.mangosos.domain.LocationMessenger
import com.bih.mangosos.domain.PhoneNumberValidator
import com.bih.mangosos.domain.SosLocation

class LocationShareHandler(
    private val context: Context,
) : LocationMessenger {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun sendLocationMessage(
        rawContacts: String,
        protectedPersonName: String,
        languageCode: String,
    ): LocationShareStatus {
        val contacts = PhoneNumberValidator.parseContacts(rawContacts)
        if (contacts.isEmpty()) return LocationShareStatus.NO_CONTACTS

        val hasLocationPermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!hasLocationPermission) {
            return LocationShareStatus.PERMISSION_DENIED
        }

        val location = getBestLastKnownLocation() ?: return LocationShareStatus.LOCATION_UNAVAILABLE
        val message = context.buildSmsEmergencyMessage(
            protectedPersonName = protectedPersonName,
            languageCode = languageCode,
            location = location,
        )

        return try {
            val recipients = contacts.joinToString(";")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.fromParts("smsto", recipients, null)
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            showToast(R.string.toast_sms_composer_opened)
            LocationShareStatus.COMPOSER_OPENED
        } catch (_: Exception) {
            LocationShareStatus.FAILED
        }
    }

    @SuppressLint("MissingPermission")
    override fun getBestLastKnownLocation(): SosLocation? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { location ->
                SosLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
            }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun showToast(messageRes: Int) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
        }
    }
}
