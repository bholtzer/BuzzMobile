package com.bih.sosguardian

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.bih.sosguardian.ui.SosApp
import com.bih.sosguardian.ui.theme.SosGuardianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            SosGuardianTheme {
                SosApp(application = application as SosApplication)
            }
        }
    }

    companion object {
        fun createLaunchIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
    }
}
