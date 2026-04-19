package com.example.sosapp.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.sosapp.data.LocationShareStatus

class LocationShareHandler(
    private val context: Context,
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun sendLocationMessage(rawContacts: String): LocationShareStatus {
        val contacts = PhoneNumberValidator.parseContacts(rawContacts)
        if (contacts.isEmpty()) return LocationShareStatus.NO_CONTACTS

        val hasLocationPermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasSmsPermission = hasPermission(Manifest.permission.SEND_SMS)
        if (!hasLocationPermission || !hasSmsPermission) {
            return LocationShareStatus.PERMISSION_DENIED
        }

        val location = getBestLastKnownLocation() ?: return LocationShareStatus.LOCATION_UNAVAILABLE
        val message = buildString {
            append("SOS Guardian alert. I may need help. ")
            append("My last known location: ")
            append("https://maps.google.com/?q=${location.latitude},${location.longitude}")
        }

        return try {
            val smsManager = SmsManager.getDefault()
            contacts.forEach { number ->
                smsManager.sendTextMessage(number, null, message, null, null)
            }
            LocationShareStatus.SENT
        } catch (_: Exception) {
            LocationShareStatus.FAILED
        }
    }

    fun getBestLastKnownLocation(): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
