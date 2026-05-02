package com.bih.mangosos.data.device

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.bih.mangosos.R
import com.bih.mangosos.domain.SosLocation
import java.util.Locale

internal fun Context.localizedEmergencyContext(languageCode: String): Context {
    val normalizedLanguage = when (languageCode) {
        "he", "iw" -> "iw"
        "es" -> "es"
        "fr" -> "fr"
        else -> "en"
    }
    val locale = Locale(normalizedLanguage)
    Locale.setDefault(locale)
    val configuration = Configuration(resources.configuration)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.setLocales(LocaleList(locale))
    } else {
        @Suppress("DEPRECATION")
        configuration.locale = locale
    }
    return createConfigurationContext(configuration)
}

internal fun Context.buildSmsEmergencyMessage(
    protectedPersonName: String,
    languageCode: String,
    location: SosLocation,
): String {
    val localizedContext = localizedEmergencyContext(languageCode)
    val displayName = protectedPersonName.ifBlank {
        localizedContext.getString(R.string.emergency_message_unknown_person)
    }
    val mapUrl = location.toMapUrl()
    return localizedContext.getString(R.string.emergency_sms_message, displayName, mapUrl)
}

internal fun Context.buildWhatsAppEmergencyMessage(
    protectedPersonName: String,
    languageCode: String,
    location: SosLocation?,
): String {
    val localizedContext = localizedEmergencyContext(languageCode)
    val displayName = protectedPersonName.ifBlank {
        localizedContext.getString(R.string.emergency_message_unknown_person)
    }
    val mapLine = location?.let {
        localizedContext.getString(R.string.emergency_message_location_line, it.toMapUrl())
    }.orEmpty()
    return localizedContext.getString(R.string.emergency_whatsapp_message, displayName, mapLine)
}

private fun SosLocation.toMapUrl(): String {
    return "https://maps.google.com/?q=$latitude,$longitude"
}
