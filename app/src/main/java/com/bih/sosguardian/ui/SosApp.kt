package com.bih.sosguardian.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bih.sosguardian.R
import com.bih.sosguardian.SosApplication
import com.bih.sosguardian.data.SosMode
import com.bih.sosguardian.data.SosSettings
import com.bih.sosguardian.domain.PhoneNumberValidator
import com.bih.sosguardian.service.SosAccessibilityService
import java.util.Locale

private val GuardianInk = Color(0xFF101828)
private val GuardianSurface = Color(0xFFFFFBF7)
private val GuardianSurfaceAlt = Color(0xFFF4F7F9)
private val GuardianRed = Color(0xFFB42318)
private val GuardianRedSoft = Color(0xFFFFE4E0)
private val GuardianTeal = Color(0xFF0F766E)
private val GuardianTealSoft = Color(0xFFDDF7F2)
private val GuardianText = Color(0xFF1F2937)
private val GuardianMuted = Color(0xFF52616B)

private data class AppLanguage(
    val code: String,
    val label: String,
)

private val supportedLanguages = listOf(
    AppLanguage("en", "English"),
    AppLanguage("he", "עברית"),
    AppLanguage("es", "Español"),
    AppLanguage("fr", "Français"),
)

@Composable
private fun stringResource(id: Int): String {
    return LocalContext.current.getString(id)
}

@Composable
private fun stringResource(id: Int, vararg formatArgs: Any): String {
    return LocalContext.current.getString(id, *formatArgs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosApp(application: SosApplication) {
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(application))
    val settings by viewModel.settings.collectAsState()
    val runtimeState by viewModel.runtimeState.collectAsState()
    val baseContext = LocalContext.current
    val activityResultRegistryOwner = checkNotNull(LocalActivityResultRegistryOwner.current) {
        "SOS Guardian requires an ActivityResultRegistryOwner."
    }
    var pendingLanguageCode by rememberSaveable { mutableStateOf<String?>(null) }
    val effectiveLanguageCode = pendingLanguageCode ?: settings.languageCode
    val context = remember(baseContext, effectiveLanguageCode) {
        baseContext.localizedContext(effectiveLanguageCode)
    }
    val localizedConfiguration = remember(context) {
        Configuration(context.resources.configuration)
    }
    val activity = baseContext.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    var permissionRequestAttempted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(settings.languageCode, pendingLanguageCode) {
        if (pendingLanguageCode != null && pendingLanguageCode == settings.languageCode) {
            pendingLanguageCode = null
        }
    }

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

    val requestPermissionsWithModernFlow = {
        val shouldExplainFirst = activity != null && permissionList.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
        if (shouldExplainFirst) {
            showPermissionRationale = true
        } else {
            permissionRequestAttempted = true
            permissionsLauncher.launch(permissionList.toTypedArray())
        }
    }
    val permissionNeedsSettings = activity != null &&
        permissionRequestAttempted &&
        permissionList.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }

    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides localizedConfiguration,
        LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.app_name)) })
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF101828), Color(0xFF3A1014), Color(0xFFF4F7F9)),
                        ),
                    )
                    .padding(innerPadding)
                    .padding(WindowInsets.safeDrawing.asPaddingValues()),
            ) {
                when {
                    runtimeState.mode == SosMode.SOS_ACTIVE || runtimeState.mode == SosMode.TRIGGER_DETECTED -> ActiveSosScreen(
                        message = runtimeState.message,
                        callStatus = runtimeState.callStatus.name.replace('_', ' '),
                        locationStatus = runtimeState.locationShareStatus.name.replace('_', ' '),
                        onStop = viewModel::stopSos,
                    )

                    !onboardingComplete || !settings.onboardingSeen -> FirstRunOnboardingScreen(
                        context = context,
                        currentSettings = settings.copy(languageCode = effectiveLanguageCode),
                        accessibilityEnabled = accessibilityEnabled,
                        batteryIgnored = batteryIgnored,
                        overlayPermission = overlayPermission,
                        criticalPermissionsGranted = criticalPermissionsGranted,
                        requestPermissions = requestPermissionsWithModernFlow,
                        permissionNeedsSettings = permissionNeedsSettings,
                        openAppSettings = { openAppSettings(context) },
                        onLanguageSelected = { languageCode ->
                            pendingLanguageCode = languageCode
                            viewModel.saveLanguage(languageCode)
                        },
                        onFinishSetup = viewModel::completeOnboarding,
                    )

                    else -> SetupScreen(
                        context = context,
                        currentSettings = settings.copy(languageCode = effectiveLanguageCode),
                        runtimeMode = runtimeState.mode,
                        onboardingComplete = onboardingComplete,
                        accessibilityEnabled = accessibilityEnabled,
                        batteryIgnored = batteryIgnored,
                        overlayPermission = overlayPermission,
                        criticalPermissionsGranted = criticalPermissionsGranted,
                        requestPermissions = requestPermissionsWithModernFlow,
                        onManualSos = viewModel::triggerManualSos,
                    )
                }
            }
        }

        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = { Text(stringResource(R.string.permission_dialog_title)) },
                text = {
                    Text(
                        stringResource(R.string.permission_dialog_body)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionRationale = false
                            permissionRequestAttempted = true
                            permissionsLauncher.launch(permissionList.toTypedArray())
                        },
                    ) {
                        Text(stringResource(R.string.onboarding_continue))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showPermissionRationale = false }) {
                        Text(stringResource(R.string.not_now))
                    }
                },
            )
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
    onManualSos: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val numberValid = PhoneNumberValidator.isValid(currentSettings.emergencyNumber)

    if (onboardingComplete) {
        EmergencyInformationScreen(
            settings = currentSettings,
            runtimeMode = runtimeMode,
            onManualSos = onManualSos,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard(enabled = currentSettings.enabled && onboardingComplete, runtimeMode = runtimeMode, onboardingComplete = onboardingComplete)
        
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

        if (!onboardingComplete) {
            PermissionsCard(
                context = context,
                accessibilityEnabled = accessibilityEnabled,
                batteryIgnored = batteryIgnored,
                overlayPermission = overlayPermission,
                criticalPermissionsGranted = criticalPermissionsGranted,
                onRequestPermissions = requestPermissions
            )
        }
        
        WarningCard()

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E151A)),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.readiness_title), color = Color(0xFFFFF7ED), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.readiness_body),
                    color = Color(0xFFFFDEC7),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button( modifier = Modifier.fillMaxWidth(),onClick = onManualSos, enabled = numberValid && onboardingComplete) { Text(stringResource(R.string.home_manual_sos)) }
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
private fun EmergencyInformationScreen(
    settings: SosSettings,
    runtimeMode: SosMode,
    onManualSos: () -> Unit,
) {
    val numberValid = PhoneNumberValidator.isValid(settings.emergencyNumber)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GuardianSurface),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(GuardianTealSoft, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = GuardianTeal,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.home_emergency_information),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = GuardianText,
                        )
                        Text(
                            "Status: ${runtimeMode.name.lowercase().replace('_', ' ')}",
                            color = GuardianMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                EmergencyInfoRow(
                    label = stringResource(R.string.home_protected_person),
                    value = settings.userName.ifBlank { stringResource(R.string.home_missing) },
                )
                EmergencyInfoRow(
                    label = stringResource(R.string.home_call_sms_contact),
                    value = settings.emergencyNumber.ifBlank { stringResource(R.string.home_missing) },
                )
                if (settings.whatsappNumber.isNotBlank()) {
                    EmergencyInfoRow(
                        label = stringResource(R.string.home_whatsapp_contact),
                        value = settings.whatsappNumber,
                    )
                }
                EmergencyInfoRow(
                    label = stringResource(R.string.home_trigger_method),
                    value = stringResource(R.string.home_trigger_body),
                )
                EmergencyInfoRow(
                    label = stringResource(R.string.home_siren),
                    value = "${(settings.sirenVolumeFraction * 100).toInt()}%",
                )
                EmergencyInfoRow(
                    label = stringResource(R.string.home_flash),
                    value = "${settings.flashBlinkMs} ms",
                )
                EmergencyInfoRow(
                    label = stringResource(R.string.home_cooldown),
                    value = "${settings.cooldownMs / 1000} sec",
                )
                EmergencyInfoRow(
                    label = stringResource(R.string.home_monitoring),
                    value = stringResource(if (settings.enabled) R.string.home_enabled else R.string.home_disabled),
                )
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            enabled = numberValid,
            onClick = onManualSos,
        ) {
            Text(stringResource(R.string.home_manual_sos))
        }
    }
}

@Composable
private fun EmergencyInfoRow(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuardianSurfaceAlt, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            color = GuardianMuted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            value,
            color = GuardianText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingIntroScreen(
    introStep: Int,
    userName: String,
    onLanguageSelected: (String) -> Unit,
    onUserNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    onStartOnboarding: () -> Unit,
) {
    val isLanguageStep = introStep == 0
    val isOpenStep = introStep == 1
    val isNameStep = introStep == 2
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                horizontal = 20.dp,
                vertical = if (!isNameStep) 18.dp else 10.dp,
            ),
        verticalArrangement = if (!isNameStep) Arrangement.Center else Arrangement.Top,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GuardianSurface),
            shape = RoundedCornerShape(if (!isNameStep) 32.dp else 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(if (!isNameStep) 28.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (!isNameStep) 18.dp else 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (!isNameStep) 74.dp else 44.dp)
                        .background(if (isOpenStep) GuardianRedSoft else GuardianTealSoft, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when {
                            isLanguageStep -> Icons.Rounded.Settings
                            isOpenStep -> Icons.Rounded.Shield
                            else -> Icons.Rounded.ContactPage
                        },
                        contentDescription = null,
                        tint = if (isOpenStep) GuardianRed else GuardianTeal,
                        modifier = Modifier.size(if (!isNameStep) 38.dp else 24.dp),
                    )
                }
                Text(
                    stringResource(
                        when {
                            isLanguageStep -> R.string.language_title
                            isOpenStep -> R.string.onboarding_open_title
                            else -> R.string.onboarding_name_title
                        },
                    ),
                    style = if (!isNameStep) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = GuardianText,
                )
                Text(
                    stringResource(
                        when {
                            isLanguageStep -> R.string.language_subtitle
                            isOpenStep -> R.string.onboarding_open_subtitle
                            else -> R.string.onboarding_name_subtitle
                        },
                    ),
                    style = if (!isNameStep) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GuardianRed,
                )
                if (isLanguageStep) {
                    supportedLanguages.forEach { language ->
                        Button(
                            onClick = {
                                onLanguageSelected(language.code)
                                onContinue()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Text(language.label)
                        }
                    }
                }
                if (isOpenStep) {
                    Text(
                        stringResource(R.string.onboarding_open_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = GuardianMuted,
                    )
                }

                if (isNameStep) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = onUserNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.onboarding_name_label)) },
                        placeholder = { Text(stringResource(R.string.onboarding_name_placeholder)) },
                        singleLine = true,
                    )
                }

                Button(
                    onClick = if (isOpenStep || isLanguageStep) onContinue else onStartOnboarding,
                    enabled = !isNameStep || userName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(
                        stringResource(if (isNameStep) R.string.onboarding_start else R.string.onboarding_continue),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupDoneScreen(
    onViewDetails: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GuardianSurface),
            shape = RoundedCornerShape(32.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .background(GuardianTealSoft, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = GuardianTeal,
                        modifier = Modifier.size(42.dp),
                    )
                }
                Text(
                    stringResource(R.string.setup_done_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = GuardianText,
                )
                Text(
                    stringResource(R.string.setup_done_subtitle),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GuardianTeal,
                )
                Text(
                    stringResource(R.string.setup_done_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = GuardianMuted,
                )
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                ) {
                    Text(stringResource(R.string.setup_done_button))
                }
            }
        }
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
    permissionNeedsSettings: Boolean,
    openAppSettings: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onFinishSetup: (SosSettings, () -> Unit) -> Unit,
) {
    data class ContactAdvanceDialogState(
        val title: String,
        val message: String,
        val nextStep: Int,
    )

    var draftUserName by rememberSaveable(currentSettings.userName) { mutableStateOf(currentSettings.userName) }
    var draftNumber by rememberSaveable(currentSettings.emergencyNumber) { mutableStateOf(currentSettings.emergencyNumber) }
    var draftWhatsappNumber by rememberSaveable(currentSettings.whatsappNumber) { mutableStateOf(currentSettings.whatsappNumber) }
    var draftVolume by rememberSaveable(currentSettings.sirenVolumeFraction) { mutableFloatStateOf(currentSettings.sirenVolumeFraction) }
    var draftBlinkMs by rememberSaveable(currentSettings.flashBlinkMs) { mutableFloatStateOf(currentSettings.flashBlinkMs.toFloat()) }
    var draftCooldownMs by rememberSaveable(currentSettings.cooldownMs) { mutableFloatStateOf(currentSettings.cooldownMs.toFloat()) }
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var introStep by rememberSaveable { mutableIntStateOf(0) }
    var showSetupDone by rememberSaveable { mutableStateOf(false) }
    var contactAdvanceDialog by remember { mutableStateOf<ContactAdvanceDialogState?>(null) }
    var showAccessibilityTutorialDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentSettings.languageCode) {
        if (currentSettings.languageCode.isBlank() && introStep != 0) {
            introStep = 0
        }
    }

    val numberValid = PhoneNumberValidator.isValid(draftNumber)
    val whatsappValid = PhoneNumberValidator.isValid(draftWhatsappNumber)
    val isWhatsappInstalled = remember {
        isAppInstalled(context, "com.whatsapp") || isAppInstalled(context, "com.whatsapp.w4b")
    }
    val hasWhatsappStep = isWhatsappInstalled
    val whatsappReady = !hasWhatsappStep || whatsappValid
    val setupReady = draftUserName.isNotBlank() && numberValid && whatsappReady && criticalPermissionsGranted && accessibilityEnabled && batteryIgnored && overlayPermission
    val totalSteps = if (hasWhatsappStep) 8 else 7
    val visibleStep = currentStep.coerceAtLeast(1)
    val visibleTotalSteps = totalSteps - 1
    val stepTitle = when (currentStep) {
        0 -> stringResource(R.string.onboarding_step_welcome)
        1 -> stringResource(R.string.onboarding_step_phone_permissions)
        2 -> stringResource(R.string.onboarding_step_accessibility)
        3 -> stringResource(R.string.onboarding_step_display_over_apps)
        4 -> stringResource(R.string.onboarding_step_battery_protection)
        5 -> stringResource(R.string.onboarding_step_call_sms_contact)
        6 -> if (hasWhatsappStep) stringResource(R.string.onboarding_step_whatsapp_contact) else stringResource(R.string.onboarding_step_alert_settings)
        else -> stringResource(R.string.onboarding_step_alert_settings)
    }
    val stepSubtitle = when (currentStep) {
        0 -> stringResource(R.string.onboarding_subtitle_start)
        1 -> stringResource(R.string.onboarding_subtitle_phone_permissions)
        2 -> stringResource(R.string.onboarding_subtitle_accessibility)
        3 -> stringResource(R.string.onboarding_subtitle_display_over_apps)
        4 -> stringResource(R.string.onboarding_subtitle_battery)
        5 -> stringResource(R.string.onboarding_subtitle_call_sms_contact)
        6 -> if (hasWhatsappStep) stringResource(R.string.onboarding_subtitle_whatsapp_contact) else stringResource(R.string.onboarding_subtitle_alert_settings)
        else -> stringResource(R.string.onboarding_subtitle_alert_settings)
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

    val completedSettings = currentSettings.copy(
        languageCode = currentSettings.languageCode.ifBlank { "en" },
        userName = draftUserName.trim(),
        emergencyNumber = draftNumber.trim(),
        whatsappNumber = draftWhatsappNumber.trim(),
        sirenVolumeFraction = draftVolume,
        flashBlinkMs = draftBlinkMs.toLong(),
        cooldownMs = draftCooldownMs.toLong(),
        enabled = setupReady,
        onboardingSeen = true,
    )

    if (introStep < 3) {
        OnboardingIntroScreen(
            introStep = introStep,
            userName = draftUserName,
            onLanguageSelected = onLanguageSelected,
            onUserNameChange = { draftUserName = it },
            onContinue = {
                if (introStep != 0 || currentSettings.languageCode.isNotBlank()) {
                    introStep += 1
                }
            },
            onStartOnboarding = {
                introStep = 3
                currentStep = 1
            },
        )
        return
    }

    if (showSetupDone) {
        SetupDoneScreen(
            onViewDetails = {
                onFinishSetup(completedSettings) {}
            },
        )
        return
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

    LaunchedEffect(currentStep, accessibilityEnabled) {
        if (currentStep == 2 && !accessibilityEnabled) {
            showAccessibilityTutorialDialog = true
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
        uri?.let {
            val phone = pickContactPhone(context, it) ?: draftNumber
            draftNumber = phone
                if (PhoneNumberValidator.isValid(phone)) {
                contactAdvanceDialog = ContactAdvanceDialogState(
                    title = context.getString(R.string.contact1_saved_title),
                    message = if (hasWhatsappStep) {
                        context.getString(R.string.contact1_saved_body_whatsapp_next)
                    } else {
                        context.getString(R.string.contact1_saved_body_final_next)
                    },
                    nextStep = if (hasWhatsappStep) 6 else totalSteps - 1,
                )
            }
        }
    }
    val whatsappPicker = rememberLauncherForActivityResult(pickPhoneContract) { uri ->
        uri?.let {
            val phone = pickContactPhone(context, it) ?: draftWhatsappNumber
            draftWhatsappNumber = phone
                if (PhoneNumberValidator.isValid(phone)) {
                contactAdvanceDialog = ContactAdvanceDialogState(
                    title = context.getString(R.string.contact2_saved_title),
                    message = context.getString(R.string.contact2_saved_body),
                    nextStep = totalSteps - 1,
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GuardianSurface),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(GuardianRedSoft, RoundedCornerShape(16.dp)),
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
                        tint = GuardianRed,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(stepTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = GuardianText)
                Text(stepSubtitle, style = MaterialTheme.typography.titleMedium, color = GuardianRed, fontWeight = FontWeight.Bold)
                Text(
                    when (currentStep) {
                        0 -> stringResource(R.string.onboarding_body_exact_press)
                        1 -> stringResource(R.string.onboarding_body_press_allow)
                        2 -> stringResource(R.string.onboarding_body_accessibility)
                        3 -> stringResource(R.string.onboarding_body_display)
                        4 -> stringResource(R.string.onboarding_body_battery)
                        5 -> stringResource(R.string.onboarding_body_choose_first_contact)
                        6 -> if (hasWhatsappStep) {
                            stringResource(R.string.onboarding_body_choose_second_contact)
                        } else {
                            stringResource(R.string.onboarding_body_finish)
                        }
                        else -> stringResource(R.string.onboarding_body_finish)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = GuardianMuted,
                )
                LinearProgressIndicator(
                    progress = { visibleStep / visibleTotalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = GuardianRed,
                    trackColor = GuardianRedSoft,
                )
                Text(
                    stringResource(R.string.onboarding_step_count, visibleStep, visibleTotalSteps),
                    color = GuardianMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (currentStep) {
                0 -> OnboardingSectionCard(
                    title = stringResource(R.string.onboarding_start_setup_title),
                    subtitle = stringResource(R.string.onboarding_start_setup_subtitle),
                ) {
                    Button(
                        onClick = { currentStep = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                    ) {
                        Text(stringResource(R.string.onboarding_continue))
                    }
                }

                1 -> OnboardingSectionCard(
                    title = stringResource(R.string.onboarding_allow_phone_title),
                    subtitle = stringResource(R.string.onboarding_allow_phone_subtitle),
                ) {
                    SingleActionStepButton(
                        enabled = !criticalPermissionsGranted,
                        done = criticalPermissionsGranted,
                        label = when {
                            criticalPermissionsGranted -> stringResource(R.string.onboarding_done)
                            permissionNeedsSettings -> stringResource(R.string.onboarding_open_settings)
                            else -> stringResource(R.string.onboarding_allow_permissions)
                        },
                        onClick = if (permissionNeedsSettings) openAppSettings else requestPermissions,
                    )
                }

                2 -> OnboardingSectionCard(
                    title = stringResource(R.string.onboarding_accessibility_title),
                    subtitle = stringResource(R.string.onboarding_accessibility_subtitle),
                ) {
                    SingleActionStepButton(
                        enabled = !accessibilityEnabled,
                        done = accessibilityEnabled,
                        label = if (accessibilityEnabled) stringResource(R.string.onboarding_done) else stringResource(R.string.onboarding_open_guardian_screen),
                        onClick = { showAccessibilityTutorialDialog = true },
                    )
                }

                3 -> OnboardingSectionCard(
                    title = stringResource(R.string.onboarding_display_title),
                    subtitle = stringResource(R.string.onboarding_display_subtitle),
                ) {
                    SingleActionStepButton(
                        enabled = !overlayPermission,
                        done = overlayPermission,
                        label = if (overlayPermission) stringResource(R.string.onboarding_done) else stringResource(R.string.onboarding_open_display_permission),
                        onClick = openOverlaySettings,
                    )
                }

                4 -> OnboardingSectionCard(
                    title = stringResource(R.string.onboarding_battery_title),
                    subtitle = stringResource(R.string.onboarding_battery_subtitle),
                ) {
                    SingleActionStepButton(
                        enabled = !batteryIgnored,
                        done = batteryIgnored,
                        label = if (batteryIgnored) stringResource(R.string.onboarding_done) else stringResource(R.string.onboarding_open_battery_screen),
                        onClick = openBatterySettings,
                    )
                }

                5 -> OnboardingSectionCard(
                    title = stringResource(R.string.onboarding_contact1_title),
                    subtitle = stringResource(R.string.onboarding_contact1_subtitle),
                ) {
                    Button(
                        onClick = { emergencyPicker.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(if (numberValid) stringResource(R.string.onboarding_choose_different_call_sms) else stringResource(R.string.onboarding_choose_call_sms))
                    }
                    if (numberValid) {
                        Text(stringResource(R.string.onboarding_selected_value, draftNumber), color = GuardianTeal, fontWeight = FontWeight.SemiBold)
                    }
                }

                6 -> if (hasWhatsappStep) OnboardingSectionCard(
                    title = stringResource(R.string.onboarding_contact2_title),
                    subtitle = stringResource(R.string.onboarding_contact2_subtitle),
                ) {
                    if (numberValid) {
                        OutlinedButton(
                            onClick = {
                                draftWhatsappNumber = draftNumber
                                contactAdvanceDialog = ContactAdvanceDialogState(
                                    title = context.getString(R.string.same_contact_title),
                                    message = context.getString(R.string.same_contact_body),
                                    nextStep = totalSteps - 1,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Text(stringResource(R.string.onboarding_use_same_contact))
                        }
                    }
                    Button(
                        onClick = { whatsappPicker.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(if (whatsappValid) stringResource(R.string.onboarding_choose_different_whatsapp) else stringResource(R.string.onboarding_choose_whatsapp))
                    }
                    if (whatsappValid) {
                        Text(stringResource(R.string.onboarding_selected_value, draftWhatsappNumber), color = GuardianTeal, fontWeight = FontWeight.SemiBold)
                    }
                } else OnboardingSectionCard(
                    title = stringResource(R.string.onboarding_step_alert_settings),
                    subtitle = stringResource(R.string.onboarding_alert_settings_subtitle),
                ) {
                    Button(
                        onClick = { showSetupDone = true },
                        enabled = setupReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                    ) {
                        Text(stringResource(R.string.onboarding_finish_setup))
                    }
                }

                else -> OnboardingSectionCard(
                    title = stringResource(R.string.onboarding_step_alert_settings),
                    subtitle = stringResource(R.string.onboarding_alert_settings_subtitle),
                ) {
                    Button(
                        onClick = { showSetupDone = true },
                        enabled = setupReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                    ) {
                        Text(stringResource(R.string.onboarding_finish_setup))
                    }
                }
            }
        }

    }

    contactAdvanceDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = {
                currentStep = dialog.nextStep
                contactAdvanceDialog = null
            },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                Button(
                    onClick = {
                        currentStep = dialog.nextStep
                        contactAdvanceDialog = null
                    },
                ) {
                    Text(stringResource(R.string.onboarding_continue))
                }
            },
        )
    }

    if (showAccessibilityTutorialDialog && !accessibilityEnabled) {
        AlertDialog(
            onDismissRequest = { showAccessibilityTutorialDialog = false },
            title = {
                Text(stringResource(R.string.onboarding_subtitle_accessibility))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(R.string.accessibility_dialog_body),
                        color = GuardianMuted,
                    )
                    PermissionHintCard(stringResource(R.string.accessibility_dialog_step1))
                    PermissionHintCard(stringResource(R.string.accessibility_dialog_step2))
                    PermissionHintCard(stringResource(R.string.accessibility_dialog_step3))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityTutorialDialog = false
                        openAccessibilitySettings()
                    },
                ) {
                    Text(stringResource(R.string.onboarding_continue))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAccessibilityTutorialDialog = false },
                ) {
                    Text(stringResource(R.string.not_now))
                }
            },
        )
    }
}

@Composable
private fun AccessibilityTutorialCard(
    stepNumber: String,
    screenTitle: String,
    actionText: String,
    helperText: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GuardianSurface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(GuardianRed, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stepNumber,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    screenTitle,
                    color = GuardianText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    actionText,
                    modifier = Modifier.weight(1f),
                    color = GuardianRed,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier
                        .background(GuardianRed, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        stringResource(R.string.press_label),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(
                helperText,
                color = GuardianMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
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
        colors = CardDefaults.cardColors(containerColor = GuardianSurface),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = GuardianText)
            Text(subtitle, color = GuardianMuted, style = MaterialTheme.typography.bodyLarge)
            content()
        }
    }
}

@Composable
private fun SingleActionStepButton(
    enabled: Boolean,
    done: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Text(label)
    }
    if (done) {
        Text(
            stringResource(R.string.completed_label),
            color = GuardianTeal,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun RuntimePermissionFlowCard(
    enabled: Boolean,
    needsSettings: Boolean,
    readyTitle: String,
    readyBody: String,
    educationalTitle: String,
    educationalBody: String,
    deniedBody: String,
    onAction: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) GuardianTealSoft else Color.White,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (enabled) GuardianTeal else GuardianRed,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.Call,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Text(
                    if (enabled) readyTitle else educationalTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GuardianText,
                )
            }

            Text(
                when {
                    enabled -> readyBody
                    needsSettings -> deniedBody
                    else -> educationalBody
                },
                color = GuardianMuted,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (!enabled) {
                if (needsSettings) {
                    PermissionHintCard(stringResource(R.string.permission_hint_open_settings))
                } else {
                    PermissionHintCard(stringResource(R.string.permission_hint_continue_allow))
                }
            }

            Button(
                onClick = if (needsSettings) onOpenSettings else onAction,
                enabled = !enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    when {
                        enabled -> stringResource(R.string.this_step_done)
                        needsSettings -> stringResource(R.string.onboarding_open_settings)
                        else -> stringResource(R.string.onboarding_continue)
                    },
                )
            }
        }
    }
}

@Composable
private fun GuidedPermissionStepCard(
    enabled: Boolean,
    readyTitle: String,
    readyBody: String,
    pendingBody: String,
    nextHint: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) GuardianTealSoft else Color.White,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (enabled) GuardianTeal else GuardianRed,
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
                    if (enabled) readyTitle else stringResource(R.string.action_needed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GuardianText,
                )
            }

            Text(
                if (enabled) readyBody else pendingBody,
                color = GuardianMuted,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (!enabled) {
                PermissionHintCard(nextHint)
            }

            Button(
                onClick = onAction,
                enabled = !enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(if (enabled) stringResource(R.string.this_step_done) else actionLabel)
            }
        }
    }
}

@Composable
private fun AccessibilityGuidanceCard(
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) GuardianTealSoft else Color.White,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (enabled) GuardianTeal else GuardianRed,
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
                    if (enabled) stringResource(R.string.accessibility_ready) else stringResource(R.string.accessibility_turn_on_switch),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GuardianText,
                )
            }

            Text(
                if (enabled) {
                    stringResource(R.string.accessibility_ready_body)
                } else {
                    stringResource(R.string.accessibility_pending_body)
                },
                color = GuardianMuted,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (!enabled) {
                PermissionHintCard(stringResource(R.string.accessibility_pending_hint))
            }

            Button(
                onClick = onAction,
                enabled = !enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(if (enabled) stringResource(R.string.this_step_done) else stringResource(R.string.onboarding_open_guardian_screen))
            }
        }
    }
}

@Composable
private fun AccessibilityServicePreviewCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = GuardianSurfaceAlt),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.accessibility_preview_title),
                fontWeight = FontWeight.Bold,
                color = GuardianText,
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        stringResource(R.string.accessibility_service_label),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = GuardianText,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GuardianRedSoft, RoundedCornerShape(20.dp))
                            .border(2.dp, GuardianRed, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.accessibility_preview_switch_title), fontWeight = FontWeight.Bold, color = GuardianRed)
                            Text(stringResource(R.string.accessibility_preview_switch_body), color = GuardianMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                        Box(
                            modifier = Modifier
                                .background(GuardianRed, RoundedCornerShape(999.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.press_here_label), color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(
                        stringResource(R.string.accessibility_preview_warning),
                        color = GuardianRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            stringResource(R.string.accessibility_preview_look_first),
                            color = GuardianRed,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleStepRow(
    number: String,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuardianRedSoft, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(GuardianRed, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
        Text(
            text,
            color = GuardianRed,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PermissionHintCard(
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GuardianSurfaceAlt, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Settings,
            contentDescription = null,
            tint = GuardianTeal,
        )
        Text(
            text,
            color = GuardianText,
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
            .background(GuardianRedSoft, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(GuardianRed, CircleShape),
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
            color = GuardianRed,
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
                .background(GuardianRed, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(
            text,
            color = GuardianText,
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
                    text = if (onboardingComplete) stringResource(R.string.system_protected) else stringResource(R.string.action_required),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
            }
            Text(
                stringResource(R.string.hero_body),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = 0.8f)
            )
            Text(
                if (enabled) stringResource(R.string.status_armed, runtimeMode.name.lowercase().replace('_', ' ')) else stringResource(R.string.status_not_armed),
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
            Text(stringResource(R.string.easy_setup_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.easy_setup_body), color = Color(0xFF6B3E26))

            GuidedStepButton(
                number = "1",
                title = stringResource(R.string.onboarding_allow_phone_title),
                subtitle = stringResource(R.string.permission_summary_phone),
                done = criticalPermissionsGranted,
                locked = false,
                onClick = onRequestPermissions,
            )
            GuidedStepButton(
                number = "2",
                title = stringResource(R.string.onboarding_subtitle_accessibility),
                subtitle = if (accessibilityEnabled) {
                    stringResource(R.string.accessibility_is_on)
                } else {
                    stringResource(R.string.accessibility_guided_subtitle)
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
                title = stringResource(R.string.onboarding_display_title),
                subtitle = stringResource(R.string.permission_summary_overlay),
                done = overlayPermission,
                locked = !accessibilityReady,
                onClick = onOpenOverlaySettings,
            )
            GuidedStepButton(
                number = "4",
                title = stringResource(R.string.onboarding_battery_title),
                subtitle = stringResource(R.string.permission_summary_battery),
                done = batteryIgnored,
                locked = !overlayReady,
                onClick = onOpenPowerSettings,
            )
            if (batteryReady) {
                Text(
                    stringResource(R.string.all_permission_steps_complete),
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
                if (done) stringResource(R.string.ok_label) else number,
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
                if (locked) stringResource(R.string.finish_previous_step_first) else subtitle,
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
                stringResource(R.string.how_to_allow_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9A3412),
            )
            Text(stringResource(R.string.how_to_allow_step1), color = Color(0xFF7C2D12))
            Text(stringResource(R.string.how_to_allow_step2), color = Color(0xFF7C2D12))
            Text(stringResource(R.string.how_to_allow_step3), color = Color(0xFF7C2D12))
            Text(stringResource(R.string.how_to_allow_step4), color = Color(0xFF7C2D12))
            Text(stringResource(R.string.how_to_allow_step5), color = Color(0xFF7C2D12))
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
                    stringResource(R.string.onboarding_progress_title),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    stringResource(R.string.onboarding_progress_count, completedCount),
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
                    0 -> stringResource(R.string.onboarding_progress_0)
                    1 -> stringResource(R.string.onboarding_progress_1)
                    2 -> stringResource(R.string.onboarding_progress_2)
                    3 -> stringResource(R.string.onboarding_progress_3)
                    else -> stringResource(R.string.onboarding_progress_done)
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
            Text(stringResource(R.string.requirements_title), color = Color.White, style = MaterialTheme.typography.headlineSmall)
            
            PermissionRow(
                label = stringResource(R.string.onboarding_subtitle_accessibility),
                complete = accessibilityEnabled, 
                icon = Icons.Rounded.Security,
                description = stringResource(R.string.requirement_accessibility_description)
            ) {
                openAccessibilityServiceSettings(context)
            }
            
            PermissionRow(
                label = stringResource(R.string.onboarding_allow_phone_title),
                complete = criticalPermissionsGranted, 
                icon = Icons.Rounded.Call,
                description = stringResource(R.string.requirement_phone_description)
            ) {
                onRequestPermissions()
            }
            
            PermissionRow(
                label = stringResource(R.string.onboarding_battery_title),
                complete = batteryIgnored, 
                icon = Icons.Rounded.NotificationsActive,
                description = stringResource(R.string.requirement_battery_description)
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
                label = stringResource(R.string.onboarding_display_title),
                complete = overlayPermission, 
                icon = Icons.Rounded.Layers,
                description = stringResource(R.string.requirement_overlay_description)
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
                contentDescription = stringResource(R.string.completed_label),
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(28.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(stringResource(R.string.setup_label), color = Color.White, style = MaterialTheme.typography.labelLarge)
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
                Text(stringResource(R.string.device_warning_title), fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.device_warning_body))
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
                Text(stringResource(R.string.sos_active_title), style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Black)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(message, color = Color(0xFFFFF0E8))
                Text(stringResource(R.string.call_status_value, callStatus), color = Color(0xFFFFD6BF))
                Text(stringResource(R.string.location_status_value, locationStatus), color = Color(0xFFFFD6BF))
                Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.stop_sos)) }
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
    val componentName = ComponentName(context, SosAccessibilityService::class.java)
    val directIntent = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
        putExtra(
            Intent.EXTRA_COMPONENT_NAME,
            componentName,
        )
        // Some settings apps respond better when the component is also provided as a string.
        putExtra("component_name", componentName.flattenToString())
        setPackage("com.android.settings")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val directIntentNoPackage = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
        putExtra(Intent.EXTRA_COMPONENT_NAME, componentName)
        putExtra("component_name", componentName.flattenToString())
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val directIntents = listOf(directIntent, directIntentNoPackage)
    val openedDirectly = directIntents.any { intent ->
        runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
    if (!openedDirectly) {
        context.startActivity(fallbackIntent)
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun Context.localizedContext(languageCode: String): Context {
    val safeLanguageCode = languageCode.ifBlank { return this }
    val locale = Locale.forLanguageTag(safeLanguageCode)
    Locale.setDefault(locale)
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.setLocales(LocaleList(locale))
    }
    return createConfigurationContext(configuration)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
