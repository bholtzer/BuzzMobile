package com.bih.sosguardian.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bih.sosguardian.SosApplication
import com.bih.sosguardian.data.SosMode
import com.bih.sosguardian.data.SosSettings
import com.bih.sosguardian.data.TriggerType
import com.bih.sosguardian.domain.PhoneNumberValidator
import com.bih.sosguardian.service.SosAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosApp(application: SosApplication) {
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(application))
    val settings by viewModel.settings.collectAsState()
    val runtimeState by viewModel.runtimeState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // State for refreshing permissions when returning to app
    var refreshTrigger by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refreshTrigger++
        onPauseOrDispose { }
    }

    val accessibilityEnabled = remember(refreshTrigger) { isAccessibilityServiceEnabled(context) }
    val batteryIgnored = remember(refreshTrigger) { isBatteryOptimizationIgnored(context) }
    val overlayPermission = remember(refreshTrigger) { isOverlayPermissionGranted(context) }
    
    val callPermission = remember(refreshTrigger) { ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED }
    val smsPermission = remember(refreshTrigger) { ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED }
    val cameraPermission = remember(refreshTrigger) { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED }
    val fineLocationPermission = remember(refreshTrigger) { ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED }
    val postNotificationsPermission = remember(refreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val criticalPermissionsGranted = callPermission && smsPermission && fineLocationPermission && 
                                   cameraPermission && postNotificationsPermission

    val onboardingComplete = accessibilityEnabled && batteryIgnored && criticalPermissionsGranted && overlayPermission

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { 
        refreshTrigger++
    }

    LaunchedEffect(runtimeState.message) {
        if (runtimeState.message.isNotBlank()) {
            snackbarHostState.showSnackbar(runtimeState.message)
        }
    }

    val permissionList = remember {
        buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
            when {
                !settings.onboardingSeen -> FirstRunOnboardingScreen(
                    context = context,
                    currentSettings = settings,
                    accessibilityEnabled = accessibilityEnabled,
                    batteryIgnored = batteryIgnored,
                    overlayPermission = overlayPermission,
                    criticalPermissionsGranted = criticalPermissionsGranted,
                    requestPermissions = { permissionsLauncher.launch(permissionList.toTypedArray()) },
                    onFinishSetup = viewModel::completeOnboarding,
                )

                runtimeState.mode == SosMode.SOS_ACTIVE || runtimeState.mode == SosMode.TRIGGER_DETECTED -> ActiveSosScreen(
                    message = runtimeState.message,
                    callStatus = runtimeState.callStatus.name.replace('_', ' '),
                    locationStatus = runtimeState.locationShareStatus.name.replace('_', ' '),
                    onStop = viewModel::stopSos,
                )

                else -> SetupScreen(
                    context = context,
                    currentSettings = settings,
                    runtimeMode = runtimeState.mode,
                    onboardingComplete = onboardingComplete,
                    accessibilityEnabled = accessibilityEnabled,
                    batteryIgnored = batteryIgnored,
                    overlayPermission = overlayPermission,
                    criticalPermissionsGranted = criticalPermissionsGranted,
                    requestPermissions = { permissionsLauncher.launch(permissionList.toTypedArray()) },
                    onSaveSettings = viewModel::saveSettings,
                    onManualTest = viewModel::triggerManualTest,
                    onManualSos = viewModel::triggerManualSos,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupScreen(
    context: Context,
    currentSettings: SosSettings,
    runtimeMode: SosMode,
    onboardingComplete: Boolean,
    accessibilityEnabled: Boolean,
    batteryIgnored: Boolean,
    overlayPermission: Boolean,
    criticalPermissionsGranted: Boolean,
    requestPermissions: () -> Unit,
    onSaveSettings: (SosSettings, () -> Unit) -> Unit,
    onManualTest: () -> Unit,
    onManualSos: () -> Unit,
) {
    var draftNumber by rememberSaveable(currentSettings.emergencyNumber) { mutableStateOf(currentSettings.emergencyNumber) }
    var draftWhatsappNumber by rememberSaveable(currentSettings.whatsappNumber) { mutableStateOf(currentSettings.whatsappNumber) }
    var draftEnabled by rememberSaveable(currentSettings.enabled) { mutableStateOf(currentSettings.enabled) }
    var draftVolume by rememberSaveable(currentSettings.sirenVolumeFraction) { mutableFloatStateOf(currentSettings.sirenVolumeFraction) }
    var draftChordWindow by rememberSaveable(currentSettings.chordWindowMs) { mutableFloatStateOf(currentSettings.chordWindowMs.toFloat()) }
    var draftBlinkMs by rememberSaveable(currentSettings.flashBlinkMs) { mutableFloatStateOf(currentSettings.flashBlinkMs.toFloat()) }
    var draftCooldownMs by rememberSaveable(currentSettings.cooldownMs) { mutableFloatStateOf(currentSettings.cooldownMs.toFloat()) }
    val scrollState = rememberScrollState()
    val numberValid = PhoneNumberValidator.isValid(draftNumber)

    val isWhatsappInstalled = remember { 
        isAppInstalled(context, "com.whatsapp") || isAppInstalled(context, "com.whatsapp.w4b") 
    }

    val pickPhoneContract = remember {
        object : ActivityResultContract<Void?, Uri?>() {
            override fun createIntent(context: Context, input: Void?): Intent =
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
                if (resultCode == Activity.RESULT_OK) intent?.data else null
        }
    }

    val pickWhatsappContract = remember {
        object : ActivityResultContract<Void?, Uri?>() {
            override fun createIntent(context: Context, input: Void?): Intent {
                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                return intent
            }
            override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
                if (resultCode == Activity.RESULT_OK) intent?.data else null
        }
    }

    val emergencyPicker = rememberLauncherForActivityResult(pickPhoneContract) { uri ->
        uri?.let { draftNumber = pickContactPhone(context, it) ?: draftNumber }
    }
    val whatsappPicker = rememberLauncherForActivityResult(pickWhatsappContract) { uri ->
        uri?.let { draftWhatsappNumber = pickContactPhone(context, it) ?: draftWhatsappNumber }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard(enabled = draftEnabled && onboardingComplete, runtimeMode = runtimeMode, onboardingComplete = onboardingComplete)
        
        if (accessibilityEnabled) {
            ShortcutWarningCard(context)
        }

        if (!onboardingComplete) {
            GuidedSetupCard(
                accessibilityEnabled = accessibilityEnabled,
                criticalPermissionsGranted = criticalPermissionsGranted,
                batteryIgnored = batteryIgnored,
                overlayPermission = overlayPermission,
                    onOpenAccessibility = { openAccessibilityServiceSettings(context) },
                onRequestPermissions = requestPermissions,
                onOpenPowerSettings = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }.onFailure {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    }
                },
                onOpenOverlaySettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = "package:${context.packageName}".toUri()
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                },
            )
            OnboardingProgressCard(
                accessibilityEnabled = accessibilityEnabled,
                batteryIgnored = batteryIgnored,
                overlayPermission = overlayPermission,
                criticalPermissionsGranted = criticalPermissionsGranted
            )
        }

        PermissionsCard(
            context = context, 
            accessibilityEnabled = accessibilityEnabled,
            batteryIgnored = batteryIgnored,
            overlayPermission = overlayPermission,
            criticalPermissionsGranted = criticalPermissionsGranted,
            onRequestPermissions = requestPermissions
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
                    label = { Text("Emergency contacts (SMS/Call)") },
                    trailingIcon = {
                        IconButton(onClick = { emergencyPicker.launch(null) }) {
                            Icon(Icons.Rounded.ContactPage, contentDescription = "Pick contact")
                        }
                    },
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

                if (isWhatsappInstalled) {
                    OutlinedTextField(
                        value = draftWhatsappNumber,
                        onValueChange = { draftWhatsappNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("WhatsApp Emergency Contact") },
                        trailingIcon = {
                            IconButton(onClick = { whatsappPicker.launch(null) }) {
                                Icon(Icons.Rounded.ContactPage, contentDescription = "Pick contact")
                            }
                        },
                        supportingText = {
                            Text("This contact will receive the photo and location alert via WhatsApp.")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1DF)),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Trigger method", fontWeight = FontWeight.Bold, color = Color(0xFF7C2D12))
                        Text(
                            "SOS starts when the user presses Volume Up + Volume Down together.",
                            color = Color(0xFF6B3E26),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                SettingSlider("Siren loudness", "${(draftVolume * 100).toInt()}%", draftVolume, 0.2f..1f) { draftVolume = it }
                SettingSlider("Button overlap window", "${draftChordWindow.toInt()} ms", draftChordWindow, 300f..1200f) { draftChordWindow = it }
                SettingSlider("Flash blink interval", "${draftBlinkMs.toInt()} ms", draftBlinkMs, 150f..1000f) { draftBlinkMs = it }
                SettingSlider("Cooldown duration", "${draftCooldownMs.toInt()} ms", draftCooldownMs, 3000f..20000f) { draftCooldownMs = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Turn on SOS trigger", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (onboardingComplete) {
                                "This lets the app listen in the background for Volume Up + Volume Down."
                            } else {
                                "Finish permissions and setup first. Then the app can listen for Volume Up + Volume Down."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (onboardingComplete) Color.Unspecified else MaterialTheme.colorScheme.error
                        )
                    }
                    Switch(
                        checked = draftEnabled && onboardingComplete, 
                        onCheckedChange = { draftEnabled = it },
                        enabled = onboardingComplete
                    )
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = numberValid,
                    onClick = {
                        val updated = currentSettings.copy(
                            emergencyNumber = draftNumber.trim(),
                            whatsappNumber = draftWhatsappNumber.trim(),
                            enabled = draftEnabled && onboardingComplete,
                            sirenVolumeFraction = draftVolume,
                            triggerType = TriggerType.VOLUME_CHORD,
                            triggerHoldMs = currentSettings.triggerHoldMs,
                            chordWindowMs = draftChordWindow.toLong(),
                            flashBlinkMs = draftBlinkMs.toLong(),
                            cooldownMs = draftCooldownMs.toLong(),
                            testMode = currentSettings.testMode,
                        )
                        onSaveSettings(updated) { draftEnabled = false }
                    },
                ) {
                    Text("Save setup")
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
                    "Use the test button first. If your device blocks lock-screen key filtering, keep the app armed in the foreground service as a fallback.",
                    color = Color(0xFFFFDEC7),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button( modifier = Modifier.fillMaxWidth(),onClick = onManualSos, enabled = numberValid && onboardingComplete) { Text("Manual SOS") }
                }
            }
        }
    }
}

private fun pickContactPhone(context: Context, contactUri: Uri): String? {
    var phone: String? = null
    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (numIdx != -1) {
                phone = cursor.getString(numIdx)
            }
        }
    }
    return phone
}

private fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        true
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstRunOnboardingScreen(
    context: Context,
    currentSettings: SosSettings,
    accessibilityEnabled: Boolean,
    batteryIgnored: Boolean,
    overlayPermission: Boolean,
    criticalPermissionsGranted: Boolean,
    requestPermissions: () -> Unit,
    onFinishSetup: (SosSettings, () -> Unit) -> Unit,
) {
    var draftNumber by rememberSaveable(currentSettings.emergencyNumber) { mutableStateOf(currentSettings.emergencyNumber) }
    var draftWhatsappNumber by rememberSaveable(currentSettings.whatsappNumber) { mutableStateOf(currentSettings.whatsappNumber) }
    var draftVolume by rememberSaveable(currentSettings.sirenVolumeFraction) { mutableFloatStateOf(currentSettings.sirenVolumeFraction) }
    var draftBlinkMs by rememberSaveable(currentSettings.flashBlinkMs) { mutableFloatStateOf(currentSettings.flashBlinkMs.toFloat()) }
    var draftCooldownMs by rememberSaveable(currentSettings.cooldownMs) { mutableFloatStateOf(currentSettings.cooldownMs.toFloat()) }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var autoOpenedPermissionSteps by rememberSaveable { mutableStateOf("") }

    val numberValid = PhoneNumberValidator.isValid(draftNumber)
    val whatsappValid = PhoneNumberValidator.isValid(draftWhatsappNumber)
    val isWhatsappInstalled = remember {
        isAppInstalled(context, "com.whatsapp") || isAppInstalled(context, "com.whatsapp.w4b")
    }
    val hasWhatsappStep = isWhatsappInstalled
    val whatsappReady = !hasWhatsappStep || whatsappValid
    val setupReady = numberValid && whatsappReady && criticalPermissionsGranted && accessibilityEnabled && batteryIgnored && overlayPermission
    val manufacturerName = remember {
        Build.MANUFACTURER.orEmpty().lowercase()
    }
    val totalSteps = if (hasWhatsappStep) 8 else 7
    val onboardingScrollState = rememberScrollState()
    val stepTitle = when (currentStep) {
        0 -> "Welcome"
        1 -> "Phone permissions"
        2 -> "Accessibility"
        3 -> "Display over apps"
        4 -> "Battery protection"
        5 -> "Call and SMS contact"
        6 -> if (hasWhatsappStep) "WhatsApp contact" else "Alert settings"
        else -> "Alert settings"
    }
    val stepSubtitle = when (currentStep) {
        0 -> "Let's get started"
        1 -> "Allow the emergency permissions"
        2 -> "Turn on SOS Guardian"
        3 -> "Let SOS show above other apps"
        4 -> "Keep SOS running in the background"
        5 -> "Choose the call and SMS emergency contact"
        6 -> if (hasWhatsappStep) "Choose the WhatsApp emergency contact" else "Pick how the SOS alert behaves"
        else -> "Pick how the SOS alert behaves"
    }

    val openAccessibilitySettings: () -> Unit = { openAccessibilityServiceSettings(context) }

    val openOverlaySettings: () -> Unit = {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    val openBatterySettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    LaunchedEffect(
        currentStep,
        criticalPermissionsGranted,
        accessibilityEnabled,
        overlayPermission,
        batteryIgnored,
    ) {
        val nextStep = when {
            currentStep == 1 && criticalPermissionsGranted -> 2
            currentStep == 2 && accessibilityEnabled -> 3
            currentStep == 3 && overlayPermission -> 4
            currentStep == 4 && batteryIgnored -> 5
            else -> null
        }
        if (nextStep != null) {
            currentStep = nextStep
        }
    }

    LaunchedEffect(
        currentStep,
        criticalPermissionsGranted,
        accessibilityEnabled,
        overlayPermission,
        batteryIgnored,
    ) {
        val shouldAutoOpen = when (currentStep) {
            1 -> !criticalPermissionsGranted
            2 -> false
            3 -> !overlayPermission
            4 -> !batteryIgnored
            else -> false
        }
        if (!shouldAutoOpen) return@LaunchedEffect

        val stepKey = "|$currentStep|"
        if (autoOpenedPermissionSteps.contains(stepKey)) return@LaunchedEffect

        autoOpenedPermissionSteps += stepKey
        when (currentStep) {
            1 -> requestPermissions()
            2 -> openAccessibilitySettings()
            3 -> openOverlaySettings()
            4 -> openBatterySettings()
        }
    }

    val pickPhoneContract = remember {
        object : ActivityResultContract<Void?, Uri?>() {
            override fun createIntent(context: Context, input: Void?): Intent =
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

            override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
                if (resultCode == Activity.RESULT_OK) intent?.data else null
        }
    }

    val emergencyPicker = rememberLauncherForActivityResult(pickPhoneContract) { uri ->
        uri?.let { draftNumber = pickContactPhone(context, it) ?: draftNumber }
    }
    val whatsappPicker = rememberLauncherForActivityResult(pickPhoneContract) { uri ->
        uri?.let { draftWhatsappNumber = pickContactPhone(context, it) ?: draftWhatsappNumber }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(onboardingScrollState)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EF)),
            shape = RoundedCornerShape(36.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .background(Color(0xFFFFE4C7), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when (currentStep) {
                            0 -> Icons.Rounded.Shield
                            1 -> Icons.Rounded.Call
                            2 -> Icons.Rounded.Lock
                            3 -> Icons.Rounded.Layers
                            4 -> Icons.Rounded.NotificationsActive
                            5 -> Icons.Rounded.ContactPage
                            6 -> if (hasWhatsappStep) Icons.Rounded.ContactPage else Icons.Rounded.Settings
                            else -> Icons.Rounded.Settings
                        },
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(38.dp),
                    )
                }
                Text(stepTitle, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text(stepSubtitle, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF6B3E26), fontWeight = FontWeight.Bold)
                Text(
                    when (currentStep) {
                        0 -> "Before we begin, we will help you set up your app in a quick and simple way."
                        1 -> "We will ask Android for the phone permissions needed for SOS."
                        2 -> "This lets the app notice the volume-button trigger."
                        3 -> "This lets the SOS screen appear on top when help is needed."
                        4 -> "This stops Android from turning the protection off."
                        5 -> "Choose the first emergency contact. This person receives the call and the SMS with location."
                        6 -> if (hasWhatsappStep) {
                            "Choose the second emergency contact. This person receives the WhatsApp message, location, and image."
                        } else {
                            "Set the siren, flash blink, and cooldown before opening the main screen."
                        }
                        else -> "Set the siren, flash blink, and cooldown before opening the main screen."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF6B3E26),
                )
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = Color(0xFFB45309),
                    trackColor = Color(0xFFFFE7D1),
                )
                Text(
                    "Step ${currentStep + 1} of $totalSteps",
                    color = Color(0xFF8A5A44),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            when (currentStep) {
                0 -> OnboardingSectionCard(
                    title = "Getting started",
                    subtitle = "Quick setup",
                ) {
                    Text("We will set up the app with you", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF20120C))
                    OnboardingChecklistItem("1", "Permission")
                    OnboardingChecklistItem("2", "Choose contact emergency to call and SMS")
                    OnboardingChecklistItem("3", "Choose contact emergency to WhatsApp")
                    OnboardingChecklistItem("4", "Set the siren loudness")
                    OnboardingChecklistItem("5", "Flash blink")
                    OnboardingChecklistItem("6", "Cooldown duration")
                }

                1 -> OnboardingSectionCard(
                    title = "Allow phone permissions",
                    subtitle = "Android will open the permission popup for you.",
                ) {
                    GuidedPermissionStepCard(
                        enabled = criticalPermissionsGranted,
                        readyTitle = "Phone permissions are ready",
                        readyBody = "Emergency calling, SMS, location, camera, and alerts are allowed.",
                        pendingBody = "The app opens the Android permission popup for you.",
                        whyItMatters = "This lets SOS call, text, get your location, and send alerts.",
                        nextHint = "Tap Allow on each Android popup.",
                        actionLabel = "Show permission popup again",
                        onAction = requestPermissions,
                    )
                }

                2 -> OnboardingSectionCard(
                    title = "Turn on SOS Guardian in Accessibility",
                    subtitle = "Follow these steps, then tap the button below.",
                ) {
                    AccessibilityGuidanceCard(
                        enabled = accessibilityEnabled,
                        manufacturerName = manufacturerName,
                        onAction = openAccessibilitySettings,
                    )
                }

                3 -> OnboardingSectionCard(
                    title = "Allow display over apps",
                    subtitle = "We will open the display setting for you.",
                ) {
                    GuidedPermissionStepCard(
                        enabled = overlayPermission,
                        readyTitle = "Display over apps is ready",
                        readyBody = "SOS can now appear above other apps.",
                        pendingBody = "The app opens the right Android screen for you.",
                        whyItMatters = "This lets the SOS screen show up quickly during an emergency.",
                        nextHint = "Turn ON Allow display over other apps.",
                        actionLabel = "Open display setting again",
                        onAction = openOverlaySettings,
                    )
                }

                4 -> OnboardingSectionCard(
                    title = "Remove battery limits",
                    subtitle = "We will open the battery setting for you.",
                ) {
                    GuidedPermissionStepCard(
                        enabled = batteryIgnored,
                        readyTitle = "Battery setting is ready",
                        readyBody = "Android is less likely to stop SOS in the background.",
                        pendingBody = "The app opens the battery screen for you.",
                        whyItMatters = "This helps SOS keep running in the background.",
                        nextHint = "Allow SOS Guardian to ignore battery limits.",
                        actionLabel = "Open battery setting again",
                        onAction = openBatterySettings,
                    )
                }

                5 -> OnboardingSectionCard(
                    title = "Emergency contact 1",
                    subtitle = "This contact receives the phone call and the SMS alert",
                ) {
                    Text(
                        "Pick one person for the emergency call and SMS. You can tap the contact button or type the number.",
                        color = Color(0xFF6B3E26),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedTextField(
                        value = draftNumber,
                        onValueChange = { draftNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Call and SMS emergency contact") },
                        trailingIcon = {
                            IconButton(onClick = { emergencyPicker.launch(null) }) {
                                Icon(Icons.Rounded.ContactPage, contentDescription = "Pick contact")
                            }
                        },
                        supportingText = {
                            Text(
                                if (numberValid) "Ready. This emergency contact will be called and will receive the SMS."
                                else "Choose one valid phone number for the emergency call and SMS.",
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = !numberValid && draftNumber.isNotBlank(),
                        singleLine = true,
                    )
                    Button(
                        onClick = { emergencyPicker.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Text(if (numberValid) "Choose a different call and SMS contact" else "Choose call and SMS contact")
                    }
                }

                6 -> if (hasWhatsappStep) OnboardingSectionCard(
                    title = "Emergency contact 2",
                    subtitle = "This contact receives the WhatsApp message, location, and image",
                ) {
                    Text(
                        "Pick one person for the WhatsApp emergency alert. This should be a second emergency contact.",
                        color = Color(0xFF6B3E26),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedTextField(
                        value = draftWhatsappNumber,
                        onValueChange = { draftWhatsappNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("WhatsApp emergency contact") },
                        trailingIcon = {
                            IconButton(onClick = { whatsappPicker.launch(null) }) {
                                Icon(Icons.Rounded.ContactPage, contentDescription = "Pick contact")
                            }
                        },
                        supportingText = {
                            Text(
                                if (whatsappValid) "Ready. This emergency contact will receive the WhatsApp alert."
                                else "Choose one valid phone number for the WhatsApp emergency contact.",
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = !whatsappValid && draftWhatsappNumber.isNotBlank(),
                        singleLine = true,
                    )
                    Button(
                        onClick = { whatsappPicker.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Text(if (whatsappValid) "Choose a different WhatsApp contact" else "Choose WhatsApp contact")
                    }
                } else OnboardingSectionCard(
                    title = "Alert settings",
                    subtitle = "Your SOS trigger is Volume Down + Volume Up",
                ) {
                    SettingSlider("Set the siren loudness", "${(draftVolume * 100).toInt()}%", draftVolume, 0.2f..1f) {
                        draftVolume = it
                    }
                    SettingSlider("Flash blink", "${draftBlinkMs.toInt()} ms", draftBlinkMs, 150f..1000f) {
                        draftBlinkMs = it
                    }
                    SettingSlider("Cooldown duration", "${draftCooldownMs.toInt()} ms", draftCooldownMs, 3000f..20000f) {
                        draftCooldownMs = it
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1DF)),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("How to use SOS", fontWeight = FontWeight.Bold, color = Color(0xFF7C2D12))
                            Text(
                                "When you need help, press Volume Down + Volume Up together.",
                                color = Color(0xFF6B3E26),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "After onboarding ends, SOS Guardian will be ready in the background.",
                                color = Color(0xFF6B3E26),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                else -> OnboardingSectionCard(
                    title = "Alert settings",
                    subtitle = "Your SOS trigger is Volume Down + Volume Up",
                ) {
                    SettingSlider("Set the siren loudness", "${(draftVolume * 100).toInt()}%", draftVolume, 0.2f..1f) {
                        draftVolume = it
                    }
                    SettingSlider("Flash blink", "${draftBlinkMs.toInt()} ms", draftBlinkMs, 150f..1000f) {
                        draftBlinkMs = it
                    }
                    SettingSlider("Cooldown duration", "${draftCooldownMs.toInt()} ms", draftCooldownMs, 3000f..20000f) {
                        draftCooldownMs = it
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1DF)),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("How to use SOS", fontWeight = FontWeight.Bold, color = Color(0xFF7C2D12))
                            Text(
                                "When you need help, press Volume Down + Volume Up together.",
                                color = Color(0xFF6B3E26),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "After onboarding ends, SOS Guardian will be ready in the background.",
                                color = Color(0xFF6B3E26),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E151A)),
            shape = RoundedCornerShape(32.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    when (currentStep) {
                        0 -> "Start the guided setup."
                        1 -> if (criticalPermissionsGranted) "Phone permissions are ready." else "Allow the emergency permissions before continuing."
                        2 -> if (accessibilityEnabled) "Accessibility is ready." else "Open Accessibility, tap SOS Guardian, then turn ON Use SOS Guardian."
                        3 -> if (overlayPermission) "Display over apps is ready." else "Allow display over apps before continuing."
                        4 -> if (batteryIgnored) "Battery protection is ready." else "Remove battery limits before continuing."
                        5 -> if (numberValid) {
                            "Emergency contact 1 is ready for call and SMS."
                        } else {
                            "Choose one valid emergency contact for the call and SMS before continuing."
                        }
                        6 -> if (hasWhatsappStep && whatsappReady) {
                            "Emergency contact 2 is ready for WhatsApp."
                        } else if (hasWhatsappStep) {
                            "Choose one valid emergency contact for WhatsApp before continuing."
                        } else if (setupReady) {
                            "Setup is ready. Press Volume Down + Volume Up together when you need SOS."
                        } else {
                            "Complete the permissions and choose your emergency contact before finishing."
                        }
                        else -> if (setupReady) {
                            "Setup is ready. Press Volume Down + Volume Up together when you need SOS."
                        } else {
                            "Complete the permissions and choose both emergency contacts before finishing."
                        }
                    },
                    color = Color(0xFFFFDEC7),
                )
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text("Back")
                    }
                }
                if (currentStep < totalSteps - 1) {
                    Button(
                        onClick = {
                            when {
                                currentStep == 1 && !criticalPermissionsGranted -> requestPermissions()
                                currentStep == 2 && !accessibilityEnabled -> openAccessibilitySettings()
                                currentStep == 3 && !overlayPermission -> openOverlaySettings()
                                currentStep == 4 && !batteryIgnored -> openBatterySettings()
                                currentStep == 5 && !numberValid -> emergencyPicker.launch(null)
                                currentStep == 6 && hasWhatsappStep && !whatsappReady -> whatsappPicker.launch(null)
                                else -> currentStep += 1
                            }
                        },
                        enabled = when (currentStep) {
                            0 -> true
                            1, 2, 3, 4 -> true
                            5 -> true
                            6 -> true
                            else -> false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(
                            when (currentStep) {
                                0 -> "Start setup"
                                1 -> if (criticalPermissionsGranted) "Continue to accessibility" else "Open permission popup again"
                                2 -> if (accessibilityEnabled) "Continue to display setting" else "Open Accessibility now"
                                3 -> if (overlayPermission) "Continue to battery setting" else "Open display setting again"
                                4 -> if (batteryIgnored) "Continue to contacts" else "Open battery setting again"
                                5 -> if (numberValid) {
                                    if (hasWhatsappStep) "Continue to WhatsApp contact" else "Go to alert settings"
                                } else {
                                    "Choose call and SMS contact"
                                }
                                6 -> if (hasWhatsappStep) {
                                    if (whatsappReady) "Go to alert settings" else "Choose WhatsApp contact"
                                } else {
                                    "Next"
                                }
                                else -> "Next"
                            },
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            onFinishSetup(
                                currentSettings.copy(
                                    emergencyNumber = draftNumber.trim(),
                                    whatsappNumber = draftWhatsappNumber.trim(),
                                    sirenVolumeFraction = draftVolume,
                                    flashBlinkMs = draftBlinkMs.toLong(),
                                    cooldownMs = draftCooldownMs.toLong(),
                                    enabled = setupReady,
                                    onboardingSeen = true,
                                ),
                            ) {}
                        },
                        enabled = setupReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text("Finish setup")
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9F2)),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color(0xFF6B3E26), style = MaterialTheme.typography.bodyLarge)
            content()
        }
    }
}

@Composable
private fun GuidedPermissionStepCard(
    enabled: Boolean,
    readyTitle: String,
    readyBody: String,
    pendingBody: String,
    whyItMatters: String,
    nextHint: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFFD9FBE6) else Color.White,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (enabled) Color(0xFF16A34A) else Color(0xFFEA580C),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Text(
                    if (enabled) readyTitle else "Action needed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20120C),
                )
            }

            Text(
                if (enabled) readyBody else pendingBody,
                color = Color(0xFF6B3E26),
                style = MaterialTheme.typography.bodyLarge,
            )

            if (!enabled) {
                PermissionWhyCard(whyItMatters)
                PermissionHintCard(nextHint)
            }

            Button(
                onClick = onAction,
                enabled = !enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(if (enabled) "This step is done" else actionLabel)
            }
        }
    }
}

@Composable
private fun AccessibilityGuidanceCard(
    enabled: Boolean,
    manufacturerName: String,
    onAction: () -> Unit,
) {
    val brandHint = when {
        "samsung" in manufacturerName -> "On Samsung, look for Installed apps, then SOS Guardian."
        "xiaomi" in manufacturerName || "redmi" in manufacturerName || "poco" in manufacturerName ->
            "On Xiaomi, Redmi, or Poco, the menu may be called Downloaded apps or Installed services."
        "oppo" in manufacturerName || "realme" in manufacturerName || "oneplus" in manufacturerName ->
            "On Oppo, Realme, or OnePlus, look for Downloaded apps or More downloaded services."
        "huawei" in manufacturerName || "honor" in manufacturerName ->
            "On Huawei or Honor, look for Installed services."
        "google" in manufacturerName || "pixel" in manufacturerName ->
            "On Pixel, open Downloaded apps, then SOS Guardian."
        else -> "If you do not see Installed apps, look for Downloaded apps or Installed services."
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFFD9FBE6) else Color.White,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (enabled) Color(0xFF16A34A) else Color(0xFFEA580C),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Text(
                    if (enabled) "Accessibility is ready" else "Tap this exact path",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20120C),
                )
            }

            Text(
                if (enabled) {
                    "SOS Guardian can now watch for the volume-button trigger."
                } else {
                    "After the Android screen opens, follow this exact path."
                },
                color = Color(0xFF6B3E26),
                style = MaterialTheme.typography.bodyLarge,
            )

            if (!enabled) {
                PermissionHintCard("Do not turn on only the shortcut. Open the SOS Guardian service and turn the service ON.")
                AccessibilityPathRow("Accessibility")
                AccessibilityPathRow("Installed apps")
                AccessibilityPathRow("SOS Guardian")
                AccessibilityPathRow("Use SOS Guardian ON")
                PermissionWhyCard("This lets the app react when both volume buttons are pressed together.")
                PermissionHintCard(brandHint)
            }

            Button(
                onClick = onAction,
                enabled = !enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(if (enabled) "This step is done" else "Open Accessibility settings")
            }
        }
    }
}

@Composable
private fun PermissionHintCard(
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEDD5), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Settings,
            contentDescription = null,
            tint = Color(0xFF9A3412),
        )
        Text(
            text,
            color = Color(0xFF9A3412),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AccessibilityPathRow(
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF1DF), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color(0xFFEA580C), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text,
            color = Color(0xFF7C2D12),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OnboardingChecklistItem(
    number: String,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color(0xFFEA580C), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(
            text,
            color = Color(0xFF20120C),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HeroCard(enabled: Boolean, runtimeMode: SosMode, onboardingComplete: Boolean) {
    val containerColor by animateColorAsState(
        targetValue = if (onboardingComplete) Color(0xFF115E59) else Color(0xB2FFF4E8),
        label = "hero_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (onboardingComplete) Color.White else Color.Black,
        label = "hero_content_color"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (onboardingComplete) Icons.Rounded.Shield else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = if (onboardingComplete) Color(0xFF2DD4BF) else Color(0xFF9A3412),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (onboardingComplete) "System Protected" else "Action Required",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
            }
            Text(
                "Hold both volume buttons together. SOS Guardian will blast sound, flash the torch, call the first saved contact, and text your location to every listed contact.",
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = 0.8f)
            )
            Text(
                if (enabled) "Status: armed (${runtimeMode.name.lowercase().replace('_', ' ')})" else "Status: not armed",
                fontWeight = FontWeight.SemiBold,
                color = if (onboardingComplete) Color(0xFFCCFBF1) else if (enabled) Color(0xFF115E59) else Color(0xFF9A3412),
            )
        }
    }
}

private enum class OnboardingPage {
    WELCOME,
    PERMISSIONS,
    SETUP,
    GUIDE,
}

@Composable
private fun OnboardingFlow(
    onFinish: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(OnboardingPage.WELCOME) }
    val stepNumber = when (page) {
        OnboardingPage.WELCOME -> 1
        OnboardingPage.PERMISSIONS -> 2
        OnboardingPage.SETUP -> 3
        OnboardingPage.GUIDE -> 4
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2E7)),
        shape = RoundedCornerShape(36.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Getting Started",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "$stepNumber of 4",
                        color = Color(0xFF8A5A44),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                LinearProgressIndicator(
                    progress = { stepNumber / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = Color(0xFFB45309),
                    trackColor = Color(0xFFE9D5C4),
                )

                when (page) {
                    OnboardingPage.WELCOME -> OnboardingWelcomePage()
                    OnboardingPage.PERMISSIONS -> OnboardingPermissionsPage()
                    OnboardingPage.SETUP -> OnboardingSetupPage()
                    OnboardingPage.GUIDE -> OnboardingGuidePage()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (page != OnboardingPage.WELCOME) {
                    OutlinedButton(
                        onClick = {
                            page = when (page) {
                                OnboardingPage.PERMISSIONS -> OnboardingPage.WELCOME
                                OnboardingPage.SETUP -> OnboardingPage.PERMISSIONS
                                OnboardingPage.GUIDE -> OnboardingPage.SETUP
                                OnboardingPage.WELCOME -> OnboardingPage.WELCOME
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Back")
                    }
                }

                Button(
                    onClick = {
                        when (page) {
                            OnboardingPage.WELCOME -> page = OnboardingPage.PERMISSIONS
                            OnboardingPage.PERMISSIONS -> page = OnboardingPage.SETUP
                            OnboardingPage.SETUP -> page = OnboardingPage.GUIDE
                            OnboardingPage.GUIDE -> onFinish()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (page == OnboardingPage.GUIDE) "Start setup" else "Next")
                }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomePage() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OnboardingHeroIcon(
            icon = Icons.Rounded.Shield,
            iconTint = Color(0xFF0F766E),
            background = Color(0xFFD9FBEF),
        )
        Text(
            "Welcome to SOS Guardian",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF20120C),
        )
        Text(
            "This app helps the user get help fast by pressing both volume buttons together.",
            color = Color(0xFF6B3E26),
            style = MaterialTheme.typography.bodyLarge,
        )
        OnboardingInfoCard(
            title = "What the app does",
            lines = listOf(
                "Calls the main emergency contact.",
                "Sends a text message with location.",
                "Can open WhatsApp with alert details.",
                "Shows the SOS screen quickly when triggered.",
            ),
        )
    }
}

@Composable
private fun OnboardingPermissionsPage() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OnboardingHeroIcon(
            icon = Icons.Rounded.Lock,
            iconTint = Color(0xFF9A3412),
            background = Color(0xFFFFE4D6),
        )
        Text(
            "Why we ask for permissions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF20120C),
        )
        Text(
            "Each permission has one simple job to help the SOS action work when it matters.",
            color = Color(0xFF6B3E26),
            style = MaterialTheme.typography.bodyLarge,
        )
        OnboardingInfoCard(
            title = "Permissions explained simply",
            lines = listOf(
                "Phone and SMS: to call and text trusted contacts.",
                "Location: to share where the user is.",
                "Camera: to attach a photo when needed.",
                "Accessibility and battery settings: to keep the trigger working in the background.",
            ),
        )
    }
}

@Composable
private fun OnboardingSetupPage() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OnboardingHeroIcon(
            icon = Icons.Rounded.Settings,
            iconTint = Color(0xFF7C3AED),
            background = Color(0xFFEDE9FE),
        )
        Text(
            "Setup takes only a few steps",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF20120C),
        )
        OnboardingInfoCard(
            title = "What to do next",
            lines = listOf(
                "1. Allow the phone permissions.",
                "2. Turn on SOS Guardian in Accessibility.",
                "3. Allow display over other apps.",
                "4. Remove battery limits.",
                "5. Add at least one emergency contact.",
            ),
        )
    }
}

@Composable
private fun OnboardingGuidePage() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OnboardingHeroIcon(
            icon = Icons.Rounded.PhoneIphone,
            iconTint = Color(0xFF1D4ED8),
            background = Color(0xFFDBEAFE),
        )
        Text(
            "How to use the app",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF20120C),
        )
        OnboardingInfoCard(
            title = "Short guidance",
            lines = listOf(
                "Save the setup after entering the contacts.",
                "Turn on SOS monitoring.",
                "Press Volume Down + Volume Up to trigger SOS.",
                "Use Run test first before depending on it.",
                "If the user is older or a child, practice once together.",
            ),
        )
    }
}

@Composable
private fun OnboardingHeroIcon(
    icon: ImageVector,
    iconTint: Color,
    background: Color,
) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .background(background, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(42.dp),
        )
    }
}

@Composable
private fun OnboardingInfoCard(
    title: String,
    lines: List<String>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF20120C),
            )
            lines.forEach { line ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(8.dp)
                            .background(Color(0xFFEA580C), CircleShape),
                    )
                    Text(
                        line,
                        color = Color(0xFF6B3E26),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutWarningCard(context: Context) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x33F97316)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().clickable {
            openAccessibilityServiceSettings(context)
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFF97316))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Action Required: Disable Shortcut",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "In Accessibility settings, turn OFF the 'Shortcut' toggle for SOS Guardian but keep the service ON.",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun GuidedSetupCard(
    accessibilityEnabled: Boolean,
    criticalPermissionsGranted: Boolean,
    batteryIgnored: Boolean,
    overlayPermission: Boolean,
    onOpenAccessibility: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenPowerSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
) {
    val permissionsReady = criticalPermissionsGranted
    val accessibilityReady = permissionsReady && accessibilityEnabled
    val overlayReady = accessibilityReady && overlayPermission
    val batteryReady = overlayReady && batteryIgnored

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E6)),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Easy setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Complete these steps in order. The next step opens after the previous one is done.", color = Color(0xFF6B3E26))

            GuidedStepButton(
                number = "1",
                title = "Allow phone permissions",
                subtitle = "Call, SMS, camera, location, and notifications.",
                done = criticalPermissionsGranted,
                locked = false,
                onClick = onRequestPermissions,
            )
            GuidedStepButton(
                number = "2",
                title = "Turn on SOS Guardian",
                subtitle = if (accessibilityEnabled) {
                    "Accessibility is on."
                } else {
                    "Tap here, then open Installed apps, choose SOS Guardian, and turn Use SOS Guardian ON."
                },
                done = accessibilityEnabled,
                locked = !permissionsReady,
                onClick = onOpenAccessibility,
            )
            if (permissionsReady && !accessibilityEnabled) {
                AccessibilityMiniHelp()
            }
            GuidedStepButton(
                number = "3",
                title = "Allow display over apps",
                subtitle = "Lets the SOS screen appear above other apps.",
                done = overlayPermission,
                locked = !accessibilityReady,
                onClick = onOpenOverlaySettings,
            )
            GuidedStepButton(
                number = "4",
                title = "Remove battery limits",
                subtitle = "Stops Android from turning the protection off.",
                done = batteryIgnored,
                locked = !overlayReady,
                onClick = onOpenPowerSettings,
            )
            if (batteryReady) {
                Text(
                    "All permission steps are complete.",
                    color = Color(0xFF166534),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun GuidedStepButton(
    number: String,
    title: String,
    subtitle: String,
    done: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (done) Color(0xFFD9FBE6) else if (locked) Color(0xFFF3F4F6) else Color.White,
                RoundedCornerShape(22.dp),
            )
            .clickable(enabled = !done && !locked) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (done) Color(0xFF16A34A) else if (locked) Color(0xFF9CA3AF) else Color(0xFFEA580C),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (done) "OK" else number,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = if (locked) Color(0xFF6B7280) else Color(0xFF20120C),
            )
            Text(
                if (locked) "Finish the previous step first." else subtitle,
                color = if (locked) Color(0xFF6B7280) else Color(0xFF6B3E26),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (done) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF16A34A),
            )
        } else if (locked) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
            )
        }
    }
}

@Composable
private fun AccessibilityMiniHelp() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDD5)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "How to allow it",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9A3412),
            )
            Text("1. Tap the orange Accessibility step above.", color = Color(0xFF7C2D12))
            Text("2. Open Installed apps or Downloaded apps.", color = Color(0xFF7C2D12))
            Text("3. Tap SOS Guardian.", color = Color(0xFF7C2D12))
            Text("4. Turn Use SOS Guardian ON.", color = Color(0xFF7C2D12))
            Text("5. If you see Allow or OK, tap it.", color = Color(0xFF7C2D12))
        }
    }
}

@Composable
private fun PermissionWhyCard(
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF1DF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Security,
            contentDescription = null,
            tint = Color(0xFF9A3412),
        )
        Text(
            text,
            color = Color(0xFF9A3412),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OnboardingProgressCard(
    accessibilityEnabled: Boolean,
    batteryIgnored: Boolean,
    overlayPermission: Boolean,
    criticalPermissionsGranted: Boolean,
) {
    val steps = listOf(accessibilityEnabled, criticalPermissionsGranted, batteryIgnored, overlayPermission)
    val completedCount = steps.count { it }
    val progress by animateFloatAsState(targetValue = completedCount / 4f, label = "onboarding_progress")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Onboarding Progress",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "$completedCount / 4 steps",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = Color(0xFF2DD4BF),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Text(
                text = when (completedCount) {
                    0 -> "Let's get you set up for safety."
                    1 -> "Good start! Three more things to do."
                    2 -> "Doing well! Two more steps."
                    3 -> "Almost there! One final step."
                    else -> "You're all set and protected."
                },
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun PermissionsCard(
    context: Context,
    accessibilityEnabled: Boolean,
    batteryIgnored: Boolean,
    overlayPermission: Boolean,
    criticalPermissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF25121D)),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Requirements", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            
            PermissionRow(
                label = "Turn on SOS Guardian", 
                complete = accessibilityEnabled, 
                icon = Icons.Rounded.Security,
                description = "Needed so the app can notice the volume-button trigger."
            ) {
                openAccessibilityServiceSettings(context)
            }
            
            PermissionRow(
                label = "Allow phone permissions",
                complete = criticalPermissionsGranted, 
                icon = Icons.Rounded.Call,
                description = "Allow calling, texting, camera, location, and notifications."
            ) {
                onRequestPermissions()
            }
            
            PermissionRow(
                label = "Remove battery limits", 
                complete = batteryIgnored, 
                icon = Icons.Rounded.NotificationsActive,
                description = "Keeps SOS Guardian working in the background."
            ) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }.onFailure {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                }
            }

            PermissionRow(
                label = "Allow display over apps", 
                complete = overlayPermission, 
                icon = Icons.Rounded.Layers,
                description = "Lets the SOS screen appear on top when needed."
            ) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
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
    complete: Boolean,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (complete) Color(0x3322C55E) else Color(0x33F97316),
        label = "row_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(enabled = !complete) { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(description, color = Color(0xFFFFE7D6), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.width(8.dp))
        if (complete) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Completed",
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(28.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Setup", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
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
        ?.any { it.resolveInfo.serviceInfo.packageName == context.packageName } ?: false
}

private fun openAccessibilityServiceSettings(context: Context) {
    val directIntent = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
        putExtra(
            Intent.EXTRA_COMPONENT_NAME,
            ComponentName(context, SosAccessibilityService::class.java),
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val packageManager = context.packageManager
    val intentToLaunch = if (directIntent.resolveActivity(packageManager) != null) {
        directIntent
    } else {
        fallbackIntent
    }

    runCatching { context.startActivity(intentToLaunch) }.onFailure {
        context.startActivity(fallbackIntent)
    }
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else {
        true
    }
}

private fun isOverlayPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}
