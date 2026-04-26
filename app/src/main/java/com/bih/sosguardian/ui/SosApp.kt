package com.bih.sosguardian.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
    var draftTriggerType by rememberSaveable(currentSettings.triggerType) { mutableStateOf(currentSettings.triggerType) }
    var draftHoldMs by rememberSaveable(currentSettings.triggerHoldMs) { mutableFloatStateOf(currentSettings.triggerHoldMs.toFloat()) }
    var draftChordWindow by rememberSaveable(currentSettings.chordWindowMs) { mutableFloatStateOf(currentSettings.chordWindowMs.toFloat()) }
    var draftBlinkMs by rememberSaveable(currentSettings.flashBlinkMs) { mutableFloatStateOf(currentSettings.flashBlinkMs.toFloat()) }
    var draftCooldownMs by rememberSaveable(currentSettings.cooldownMs) { mutableFloatStateOf(currentSettings.cooldownMs.toFloat()) }
    var draftTestMode by rememberSaveable(currentSettings.testMode) { mutableStateOf(currentSettings.testMode) }

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

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when(draftTriggerType) {
                            TriggerType.VOLUME_CHORD -> "Volume Up + Down"
                            TriggerType.VOL_UP_LONG_PRESS -> "Volume Up (Long Press)"
                            TriggerType.VOL_DOWN_LONG_PRESS -> "Volume Down (Long Press)"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trigger Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Volume Up + Down") },
                            onClick = { draftTriggerType = TriggerType.VOLUME_CHORD; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Volume Up (Long Press)") },
                            onClick = { draftTriggerType = TriggerType.VOL_UP_LONG_PRESS; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Volume Down (Long Press)") },
                            onClick = { draftTriggerType = TriggerType.VOL_DOWN_LONG_PRESS; expanded = false }
                        )
                    }
                }

                SettingSlider("Siren loudness", "${(draftVolume * 100).toInt()}%", draftVolume, 0.2f..1f) { draftVolume = it }
                SettingSlider("Hold duration", "${draftHoldMs.toInt()} ms", draftHoldMs, 400f..3000f) { draftHoldMs = it }
                if (draftTriggerType == TriggerType.VOLUME_CHORD) {
                    SettingSlider("Button overlap window", "${draftChordWindow.toInt()} ms", draftChordWindow, 300f..1200f) { draftChordWindow = it }
                }
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
                            if (onboardingComplete) "Ready to be armed." 
                            else "Requires accessibility, all permissions, and battery exclusion.", 
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
                            triggerType = draftTriggerType,
                            triggerHoldMs = draftHoldMs.toLong(),
                            chordWindowMs = draftChordWindow.toLong(),
                            flashBlinkMs = draftBlinkMs.toLong(),
                            cooldownMs = draftCooldownMs.toLong(),
                            testMode = draftTestMode,
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
                    OutlinedButton(onClick = onManualTest) { Text("Run test") }
                    Button(onClick = onManualSos, enabled = numberValid && onboardingComplete) { Text("Manual SOS") }
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

@Composable
private fun ShortcutWarningCard(context: Context) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x33F97316)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().clickable {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
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
                label = "Accessibility trigger service", 
                complete = accessibilityEnabled, 
                icon = Icons.Rounded.Security,
                description = "Required to detect volume button presses while the screen is off."
            ) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            
            PermissionRow(
                label = "Emergency permissions",
                complete = criticalPermissionsGranted, 
                icon = Icons.Rounded.Call,
                description = "SMS, Call, Camera, and Location are needed for full SOS functionality."
            ) {
                onRequestPermissions()
            }
            
            PermissionRow(
                label = "Battery optimization exclusion", 
                complete = batteryIgnored, 
                icon = Icons.Rounded.NotificationsActive,
                description = "Prevents the system from killing the SOS monitoring service."
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
                label = "Display over other apps", 
                complete = overlayPermission, 
                icon = Icons.Rounded.Layers,
                description = "Allows the SOS screen to appear even when your phone is locked or in another app."
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
