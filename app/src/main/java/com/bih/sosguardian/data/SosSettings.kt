package com.bih.sosguardian.data

enum class TriggerType {
    VOLUME_CHORD,
    VOL_UP_LONG_PRESS,
    VOL_DOWN_LONG_PRESS,
}

data class SosSettings(
    val emergencyNumber: String = "",
    val whatsappNumber: String = "",
    val enabled: Boolean = false,
    val onboardingSeen: Boolean = false,
    val sirenVolumeFraction: Float = 1f,
    val triggerType: TriggerType = TriggerType.VOLUME_CHORD,
    val triggerHoldMs: Long = 500L,
    val chordWindowMs: Long = 600L,
    val flashBlinkMs: Long = 350L,
    val cooldownMs: Long = 5000L,
    val testMode: Boolean = true,
)

enum class SosMode {
    IDLE,
    ARMED,
    TRIGGER_DETECTED,
    SOS_ACTIVE,
    COOLDOWN,
}

enum class TriggerSource {
    HARDWARE_BUTTONS,
    MANUAL_TEST,
    MANUAL_SOS,
}

enum class StopReason {
    USER_STOPPED,
    COOLDOWN_COMPLETE,
    ERROR,
}

enum class CallStatus {
    IDLE,
    PLACED_DIRECT_CALL,
    OPENED_DIALER_FALLBACK,
    SKIPPED_TEST_MODE,
    FAILED,
}

enum class LocationShareStatus {
    IDLE,
    SENT,
    SKIPPED_TEST_MODE,
    PERMISSION_DENIED,
    LOCATION_UNAVAILABLE,
    NO_CONTACTS,
    FAILED,
}

data class SosRuntimeState(
    val mode: SosMode = SosMode.IDLE,
    val sirenActive: Boolean = false,
    val flashActive: Boolean = false,
    val callStatus: CallStatus = CallStatus.IDLE,
    val locationShareStatus: LocationShareStatus = LocationShareStatus.IDLE,
    val lastSource: TriggerSource? = null,
    val message: String = "SOS is idle.",
)
