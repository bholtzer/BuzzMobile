package com.bih.mangosos.data.device

import android.content.Context
import android.os.Bundle
import com.bih.mangosos.domain.AnalyticsTracker
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsTracker(
    context: Context,
    private val fallback: AnalyticsTracker,
) : AnalyticsTracker {
    private val appContext = context.applicationContext

    override fun track(
        event: String,
        properties: Map<String, String>,
    ) {
        runCatching {
            val bundle = Bundle().apply {
                properties.forEach { (key, value) ->
                    putString(key, value.take(MAX_PROPERTY_VALUE_LENGTH))
                }
            }
            FirebaseAnalytics.getInstance(appContext).logEvent(event, bundle)
        }.onFailure {
            fallback.track(event, properties)
        }
    }

    private companion object {
        const val MAX_PROPERTY_VALUE_LENGTH = 100
    }
}
