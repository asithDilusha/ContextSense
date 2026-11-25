package com.group8.phonecontext.domain.model

/**
 * A single raw multi-sensor snapshot stamped with both wall-clock time (ms)
 * and the hardware monotonic sensor event timestamp (ns) to verify actual Hz.
 */
data class SensorReading(
    val wallTimeMs: Long,
    val sensorTimestampNs: Long,
    val ax: Float,
    val ay: Float,
    val az: Float,
    val lightLux: Float,
    val proximityCm: Float,
    val proximityState: ProximityState
)

/**
 * Hardware availability verification for the three study sensors.
 */
data class SensorAvailability(
    val hasAccelerometer: Boolean = false,
    val hasLight: Boolean = false,
    val hasProximity: Boolean = false
) {
    val allAvailable: Boolean
        get() = hasAccelerometer && hasLight && hasProximity
}
