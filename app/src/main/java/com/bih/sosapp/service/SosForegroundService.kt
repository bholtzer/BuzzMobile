package com.bih.sosapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bih.sosapp.MainActivity
import com.bih.sosapp.SosApplication
import com.bih.sosapp.data.SosMode
import com.bih.sosapp.data.StopReason
import com.example.sosapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SosForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appContainer by lazy { (application as SosApplication).appContainer }
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        serviceScope.launch {
            appContainer.sosCoordinator.runtimeState.collectLatest {
                safeStartForeground(buildNotification())
                if (!appContainer.settingsStore.settings.value.enabled && it.mode == SosMode.IDLE) {
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SOS -> appContainer.sosCoordinator.stopSos(StopReason.USER_STOPPED)
            ACTION_STOP_MONITORING -> {
                serviceScope.launch {
                    appContainer.settingsStore.updateSettings { current -> current.copy(enabled = false) }
                    appContainer.sosCoordinator.setArmed(enabled = false)
                    stopSelf()
                }
            }
            ACTION_SYNC_NOTIFICATION, ACTION_START_MONITORING, null -> {
                safeStartForeground(buildNotification())
            }
        }
        return START_STICKY
    }

    private fun safeStartForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val runtimeState = appContainer.sosCoordinator.runtimeState.value
        val mainIntent = PendingIntent.getActivity(
            this,
            1,
            MainActivity.Companion.createLaunchIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, SosForegroundService::class.java).apply { action = ACTION_STOP_SOS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val disableIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, SosForegroundService::class.java).apply { action = ACTION_STOP_MONITORING },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val channelId = if (runtimeState.mode == SosMode.SOS_ACTIVE) ACTIVE_CHANNEL_ID else MONITORING_CHANNEL_ID
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                if (runtimeState.mode == SosMode.SOS_ACTIVE) "SOS active" else "SOS monitoring armed",
            )
            .setContentText(runtimeState.message)
            .setContentIntent(mainIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .apply {
                if (runtimeState.mode == SosMode.SOS_ACTIVE) {
                    addAction(0, "Stop SOS", stopIntent)
                } else {
                    addAction(0, "Disable", disableIntent)
                }
            }
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                MONITORING_CHANNEL_ID,
                getString(R.string.monitoring_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.monitoring_channel_description)
            },
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                ACTIVE_CHANNEL_ID,
                getString(R.string.active_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.active_channel_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    companion object {
        const val ACTION_START_MONITORING = "com.example.sosapp.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.sosapp.action.STOP_MONITORING"
        const val ACTION_SYNC_NOTIFICATION = "com.example.sosapp.action.SYNC_NOTIFICATION"
        const val ACTION_STOP_SOS = "com.example.sosapp.action.STOP_SOS"
        private const val MONITORING_CHANNEL_ID = "sos_monitoring"
        private const val ACTIVE_CHANNEL_ID = "sos_active"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, SosForegroundService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, SosForegroundService::class.java).apply {
                    action = ACTION_STOP_MONITORING
                },
            )
        }
    }
}
