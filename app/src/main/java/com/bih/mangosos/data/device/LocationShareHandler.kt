package com.bih.mangosos.data.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
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
        val hasSmsPermission = hasPermission(Manifest.permission.SEND_SMS)
        if (!hasLocationPermission || !hasSmsPermission) {
            return LocationShareStatus.PERMISSION_DENIED
        }

        val location = getBestLastKnownLocation() ?: return LocationShareStatus.LOCATION_UNAVAILABLE
        val message = context.buildSmsEmergencyMessage(
            protectedPersonName = protectedPersonName,
            languageCode = languageCode,
            location = location,
        )

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
}
