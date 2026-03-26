package com.example.sosapp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.sosapp.ui.SosApp
import com.example.sosapp.ui.theme.SosGuardianTheme

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
