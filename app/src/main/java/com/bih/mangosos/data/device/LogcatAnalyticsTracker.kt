package com.bih.mangosos.data.device

import android.util.Log
import com.bih.mangosos.domain.AnalyticsTracker

class LogcatAnalyticsTracker : AnalyticsTracker {
    override fun track(
        event: String,
        properties: Map<String, String>,
    ) {
        val safeProperties = properties.entries.joinToString(
            separator = ", ",
            prefix = "{",
            postfix = "}",
        ) { (key, value) -> "$key=$value" }
        Log.d("MangoAnalytics", "$event $safeProperties")
    }
}
