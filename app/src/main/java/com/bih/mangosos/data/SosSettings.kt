package com.bih.mangosos.data

enum class TriggerType {
    VOLUME_CHORD,
    VOL_UP_LONG_PRESS,
    VOL_DOWN_LONG_PRESS,
}

data class SosSettings(
    val languageCode: String = "",
    val userName: String = "",
    val emergencyNumber: String = "",
    val emergencyContactName: String = "",
    val whatsappNumber: String = "",
    val whatsappContactName: String = "",
    val enabled: Boolean = false,
    val onboardingSeen: Boolean = false,
    val analyticsEnabled: Boolean = false,
    val sirenVolumeFraction: Float = 1f,
    val triggerType: TriggerType = TriggerType.VOLUME_CHORD,
    val triggerHoldMs: Long = 2000L,
    val chordWindowMs: Long = 150L,
    val flashBlinkMs: Long = 350L,
    val cooldownMs: Long = 5000L,
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
    FAILED,
}

enum class LocationShareStatus {
    IDLE,
    SENT,
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
