package com.example.sosapp.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sosapp.SosApplication
import com.example.sosapp.data.SosMode
import com.example.sosapp.data.SosSettings
import com.example.sosapp.domain.PhoneNumberValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosApp(application: SosApplication) {
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(application))
    val settings by viewModel.settings.collectAsState()
    val runtimeState by viewModel.runtimeState.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    var permissionFeedback by remember { mutableStateOf<String?>(null) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionFeedback = if (granted) {
            "Call permission granted."
        } else {
            if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE) == false) {
                "Call permission was denied permanently. Open app settings to enable it."
            } else {
                "Call permission is still missing."
            }
        }
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionFeedback = if (granted) {
            "SMS permission granted."
        } else {
            if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS) == false) {
                "SMS permission was denied permanently. Open app settings to enable it."
            } else {
                "SMS permission is still missing."
            }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionFeedback = if (granted) {
            "Location permission granted."
        } else {
            val permanentlyDenied = activity?.let {
                !it.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    !it.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            } == true
            if (permanentlyDenied) {
                "Location permission was denied permanently. Open app settings to enable it."
            } else {
                "Location permission is still missing."
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionFeedback = if (granted) {
            "Camera permission granted."
        } else {
            if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false) {
                "Camera permission was denied permanently. Open app settings to enable it."
            } else {
                "Camera permission is still missing."
            }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionFeedback = if (granted) "Notification permission granted." else "Notification permission is still missing."
    }

    LaunchedEffect(runtimeState.message, permissionFeedback) {
        val message = permissionFeedback ?: runtimeState.message
        snackbarHostState.showSnackbar(message)
        permissionFeedback = null
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SOS Guardian") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF140B13), Color(0xFF401122), Color(0xFFF8E9D8)),
                    ),
                )
                .padding(innerPadding)
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
        ) {
            when (runtimeState.mode) {
                SosMode.SOS_ACTIVE, SosMode.TRIGGER_DETECTED -> ActiveSosScreen(
                    message = runtimeState.message,
                    callStatus = runtimeState.callStatus.name.replace('_', ' '),
                    locationStatus = runtimeState.locationShareStatus.name.replace('_', ' '),
                    onStop = viewModel::stopSos,
                )

                else -> SetupScreen(
                    context = context,
                    currentSettings = settings,
                    runtimeMode = runtimeState.mode,
                    callPermissionAction = buildRuntimePermissionAction(
                        context = context,
                        activity = activity,
                        permission = Manifest.permission.CALL_PHONE,
                        launchRequest = { callPermissionLauncher.launch(Manifest.permission.CALL_PHONE) },
                    ),
                    smsPermissionAction = buildRuntimePermissionAction(
                        context = context,
                        activity = activity,
                        permission = Manifest.permission.SEND_SMS,
                        launchRequest = { smsPermissionLauncher.launch(Manifest.permission.SEND_SMS) },
                    ),
                    locationPermissionAction = buildLocationPermissionAction(
                        context = context,
                        activity = activity,
                        launchRequest = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        },
                    ),
                    cameraPermissionAction = buildRuntimePermissionAction(
                        context = context,
                        activity = activity,
                        permission = Manifest.permission.CAMERA,
                        launchRequest = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    ),
                    requestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            permissionFeedback = "Notification permission is not needed on this Android version."
                        }
                    },
                    onSaveSettings = viewModel::saveSettings,
                    onManualTest = viewModel::triggerManualTest,
                    onManualSos = viewModel::triggerManualSos,
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(
    context: Context,
    currentSettings: SosSettings,
    runtimeMode: SosMode,
    callPermissionAction: PermissionAction,
    smsPermissionAction: PermissionAction,
    locationPermissionAction: PermissionAction,
    cameraPermissionAction: PermissionAction,
    requestNotificationPermission: () -> Unit,
    onSaveSettings: (SosSettings, () -> Unit) -> Unit,
    onManualTest: () -> Unit,
    onManualSos: () -> Unit,
) {
    var draftNumber by rememberSaveable(currentSettings.emergencyNumber) { mutableStateOf(currentSettings.emergencyNumber) }
    var draftEnabled by rememberSaveable(currentSettings.enabled) { mutableStateOf(currentSettings.enabled) }
    var draftVolume by rememberSaveable(currentSettings.sirenVolumeFraction) { mutableStateOf(currentSettings.sirenVolumeFraction) }
    var draftHoldMs by rememberSaveable(currentSettings.triggerHoldMs) { mutableStateOf(currentSettings.triggerHoldMs.toFloat()) }
    var draftChordWindow by rememberSaveable(currentSettings.chordWindowMs) { mutableStateOf(currentSettings.chordWindowMs.toFloat()) }
    var draftBlinkMs by rememberSaveable(currentSettings.flashBlinkMs) { mutableStateOf(currentSettings.flashBlinkMs.toFloat()) }
    var draftCooldownMs by rememberSaveable(currentSettings.cooldownMs) { mutableStateOf(currentSettings.cooldownMs.toFloat()) }
    var draftTestMode by rememberSaveable(currentSettings.testMode) { mutableStateOf(currentSettings.testMode) }

    val scrollState = rememberScrollState()
    val numberValid = PhoneNumberValidator.isValid(draftNumber)
    val permissionState = rememberPermissionState(context)
    val setupReady = numberValid &&
        permissionState.accessibilityEnabled &&
        permissionState.callPermission &&
        permissionState.smsPermission &&
        permissionState.locationPermission &&
        permissionState.cameraPermission &&
        permissionState.batteryOptimizationIgnored

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard(enabled = draftEnabled, runtimeMode = runtimeMode)
        PermissionsCard(
            context = context,
            permissionState = permissionState,
            callPermissionAction = callPermissionAction,
            smsPermissionAction = smsPermissionAction,
            locationPermissionAction = locationPermissionAction,
            cameraPermissionAction = cameraPermissionAction,
            requestNotificationPermission = requestNotificationPermission,
        )
        WarningCard()

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F2EA)),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Emergency setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = draftNumber,
                    onValueChange = { draftNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Emergency contacts") },
                    supportingText = {
                        Text(
                            if (numberValid) {
                                "First contact is used for the direct call. Every listed contact receives the location SMS."
                            } else {
                                "Enter one or more valid phone numbers separated by commas, semicolons, or new lines."
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = !numberValid && draftNumber.isNotBlank(),
                )

                SettingSlider("Siren loudness", "${(draftVolume * 100).toInt()}%", draftVolume, 0.2f..1f) { draftVolume = it }
                SettingSlider("Hold duration", "${draftHoldMs.toInt()} ms", draftHoldMs, 800f..3000f) { draftHoldMs = it }
                SettingSlider("Button overlap window", "${draftChordWindow.toInt()} ms", draftChordWindow, 300f..1200f) { draftChordWindow = it }
                SettingSlider("Flash blink interval", "${draftBlinkMs.toInt()} ms", draftBlinkMs, 150f..1000f) { draftBlinkMs = it }
                SettingSlider("Cooldown duration", "${draftCooldownMs.toInt()} ms", draftCooldownMs, 3000f..20000f) { draftCooldownMs = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Test mode", fontWeight = FontWeight.SemiBold)
                        Text("When on, the app skips real calls and only runs siren + flashlight.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = draftTestMode, onCheckedChange = { draftTestMode = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Arm SOS monitoring", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (setupReady) {
                                "Everything required for SOS is ready."
                            } else {
                                "Finish the checklist above before arming SOS monitoring."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = draftEnabled,
                        onCheckedChange = { checked ->
                            draftEnabled = if (checked && !setupReady) false else checked
                        },
                    )
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = numberValid,
                    onClick = {
                        val updated = currentSettings.copy(
                            emergencyNumber = draftNumber.trim(),
                            enabled = draftEnabled && setupReady,
                            sirenVolumeFraction = draftVolume,
                            triggerHoldMs = draftHoldMs.toLong(),
                            chordWindowMs = draftChordWindow.toLong(),
                            flashBlinkMs = draftBlinkMs.toLong(),
                            cooldownMs = draftCooldownMs.toLong(),
                            testMode = draftTestMode,
                        )
                        onSaveSettings(updated) { draftEnabled = false }
                    },
                ) {
                    Text(if (setupReady) "Save and arm setup" else "Save setup")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E151A)),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Readiness", color = Color(0xFFFFF7ED), style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (setupReady) {
                        "Use the test button first. If your device blocks lock-screen key filtering, keep the app armed in the foreground service as a fallback."
                    } else {
                        "Follow the checklist from top to bottom: Accessibility, Call, SMS, Location, Camera, Battery optimization, then save and arm."
                    },
                    color = Color(0xFFFFDEC7),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onManualTest) { Text("Run test") }
                    Button(onClick = onManualSos, enabled = numberValid) { Text("Manual SOS") }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(enabled: Boolean, runtimeMode: SosMode) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xB2FFF4E8)),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Emergency spotlight", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(
                "Hold both volume buttons together. SOS Guardian will blast sound, flash the torch, call the first saved contact, and text your location to every listed contact.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                if (enabled) "Status: armed (${runtimeMode.name.lowercase().replace('_', ' ')})" else "Status: not armed",
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color(0xFF115E59) else Color(0xFF9A3412),
            )
        }
    }
}

@Composable
private fun PermissionsCard(
    context: Context,
    permissionState: PermissionState,
    callPermissionAction: PermissionAction,
    smsPermissionAction: PermissionAction,
    locationPermissionAction: PermissionAction,
    cameraPermissionAction: PermissionAction,
    requestNotificationPermission: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF25121D)),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("SOS checklist", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Complete these steps in order. Each permission unlocks a specific SOS action, so the user always knows what is still missing.",
                color = Color(0xFFFFE7D6),
                style = MaterialTheme.typography.bodyMedium,
            )
            PermissionRow(
                label = "1. Accessibility trigger service",
                detail = "Needed to detect the volume-button SOS gesture while the app is in the background.",
                complete = permissionState.accessibilityEnabled,
                icon = Icons.Rounded.Security,
            ) {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PermissionRow(
                label = "2. Call permission",
                detail = "Lets SOS call the first emergency contact automatically.",
                complete = permissionState.callPermission,
                icon = Icons.Rounded.PhoneInTalk,
                buttonLabel = if (permissionState.callPermission) "Review" else callPermissionAction.label,
                onClick = callPermissionAction.onClick,
            )
            PermissionRow(
                label = "3. SMS permission",
                detail = "Lets SOS text the location link to every emergency contact.",
                complete = permissionState.smsPermission,
                icon = Icons.Rounded.Sms,
                buttonLabel = if (permissionState.smsPermission) "Review" else smsPermissionAction.label,
                onClick = smsPermissionAction.onClick,
            )
            PermissionRow(
                label = "4. Location permission",
                detail = "Lets SOS attach the last known location to the emergency SMS.",
                complete = permissionState.locationPermission,
                icon = Icons.Rounded.LocationOn,
                buttonLabel = if (permissionState.locationPermission) "Review" else locationPermissionAction.label,
                onClick = locationPermissionAction.onClick,
            )
            PermissionRow(
                label = "5. Camera / flashlight permission",
                detail = "Lets SOS blink the flashlight to help people find the user.",
                complete = permissionState.cameraPermission,
                icon = Icons.Rounded.FlashOn,
                buttonLabel = if (permissionState.cameraPermission) "Review" else cameraPermissionAction.label,
                onClick = cameraPermissionAction.onClick,
            )
            if (permissionState.notificationsRequired) {
                PermissionRow(
                    label = "6. Notification permission",
                    detail = "Lets Android show ongoing SOS monitoring and active emergency notifications.",
                    complete = permissionState.notificationPermission,
                    icon = Icons.Rounded.NotificationsActive,
                    onClick = requestNotificationPermission,
                )
            }
            PermissionRow(
                label = if (permissionState.notificationsRequired) "7. Battery optimization exclusion" else "6. Battery optimization exclusion",
                detail = "Improves reliability so Android is less likely to stop background SOS monitoring.",
                complete = permissionState.batteryOptimizationIgnored,
                icon = Icons.Rounded.BatterySaver,
            ) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    detail: String,
    complete: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonLabel: String = if (complete) "Review" else "Open",
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (complete) Color(0x3322C55E) else Color(0x33F97316),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Color(0xFFFFE7D6), style = MaterialTheme.typography.bodySmall)
                Text(if (complete) "Ready" else "Action needed", color = Color(0xFFFFE7D6))
            }
        }
        OutlinedButton(onClick = onClick) {
            Text(buttonLabel)
        }
    }
}

@Composable
private fun WarningCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE7D1)),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FlashOn, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Device-specific warning", fontWeight = FontWeight.Bold)
            }
            Text("Android vendors differ. Some phones may not pass both volume-button events while locked. This app keeps a foreground monitoring mode as the fallback.")
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(valueLabel)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
private fun ActiveSosScreen(
    message: String,
    callStatus: String,
    locationStatus: String,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF5B1020)),
            shape = RoundedCornerShape(36.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = Color(0xFFFFE6D5))
                Text("SOS ACTIVE", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(message, color = Color(0xFFFFF0E8))
                Text("Call status: $callStatus", color = Color(0xFFFFD6BF))
                Text("Location status: $locationStatus", color = Color(0xFFFFD6BF))
                Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Stop SOS") }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }
}

private data class PermissionState(
    val accessibilityEnabled: Boolean,
    val callPermission: Boolean,
    val smsPermission: Boolean,
    val locationPermission: Boolean,
    val cameraPermission: Boolean,
    val notificationPermission: Boolean,
    val notificationsRequired: Boolean,
    val batteryOptimizationIgnored: Boolean,
)

private data class PermissionAction(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
private fun rememberPermissionState(context: Context): PermissionState {
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    refreshTick

    return PermissionState(
        accessibilityEnabled = isAccessibilityServiceEnabled(context),
        callPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED,
        smsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED,
        locationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        },
        notificationsRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        batteryOptimizationIgnored = isBatteryOptimizationIgnored(context),
    )
}

private fun buildRuntimePermissionAction(
    context: Context,
    activity: Activity?,
    permission: String,
    launchRequest: () -> Unit,
): PermissionAction {
    val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    if (granted) {
        return PermissionAction(label = "Review", onClick = { openAppSettings(context) })
    }

    val permanentlyDenied = activity?.let {
        !it.shouldShowRequestPermissionRationale(permission)
    } == true

    return if (permanentlyDenied) {
        PermissionAction(label = "Settings", onClick = { openAppSettings(context) })
    } else {
        PermissionAction(label = "Allow", onClick = launchRequest)
    }
}

private fun buildLocationPermissionAction(
    context: Context,
    activity: Activity?,
    launchRequest: () -> Unit,
): PermissionAction {
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (fineGranted || coarseGranted) {
        return PermissionAction(label = "Review", onClick = { openAppSettings(context) })
    }

    val permanentlyDenied = activity?.let {
        !it.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !it.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
    } == true

    return if (permanentlyDenied) {
        PermissionAction(label = "Settings", onClick = { openAppSettings(context) })
    } else {
        PermissionAction(label = "Allow", onClick = launchRequest)
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
