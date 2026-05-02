package com.bih.mangosos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.bih.mangosos.data.TriggerSource
import com.bih.mangosos.ui.SosApp
import com.bih.mangosos.ui.theme.MangoSosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            MangoSosTheme {
                SosApp(application = application as SosApplication)
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_TRIGGER_SOS, false) == true) {
            intent.removeExtra(EXTRA_TRIGGER_SOS)
            (application as SosApplication).appContainer.sosCoordinator.startSos(TriggerSource.HARDWARE_BUTTONS)
        }
    }

    companion object {
        private const val EXTRA_TRIGGER_SOS = "com.bih.mangosos.extra.TRIGGER_SOS"

        fun createLaunchIntent(
            context: Context,
            triggerSos: Boolean = false,
        ): Intent = Intent(context, MainActivity::class.java).apply {
            if (triggerSos) {
                putExtra(EXTRA_TRIGGER_SOS, true)
            }
        }
    }
}
