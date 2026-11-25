package com.group8.phonecontext.domain.model

/**
 * Computed features and rule-based prediction for a single 3.0-second non-overlapping window.
 */
data class SensorWindow(
    val windowIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val sampleCount: Int,
    val requestedHz: SamplingRateHz,
    val actualHz: Double,
    val accelMeanX: Float,
    val accelMeanY: Float,
    val accelMeanZ: Float,
    val movementVariance: Double,
    val meanLightLux: Float,
    val meanProximityCm: Float,
    val proximityState: ProximityState,
    val predictedContext: ContextType
)

/**
 * Single persisted row matching the exact 15-column CSV schema for Python analysis:
 * timestamp_ms,run_id,window_index,requested_hz,actual_hz,true_label,predicted_label,
 * accel_mean_x,accel_mean_y,accel_mean_z,movement_variance,light_lux,proximity_cm,
 * proximity_state,dnd_active
 */
data class LoggedWindowRecord(
    val timestampMs: Long,
    val runId: String,
    val windowIndex: Int,
    val requestedHz: Int,
    val actualHz: Double,
    val trueLabel: ContextType,
    val predictedLabel: ContextType,
    val accelMeanX: Float,
    val accelMeanY: Float,
    val accelMeanZ: Float,
    val movementVariance: Double,
    val lightLux: Float,
    val proximityCm: Float,
    val proximityState: ProximityState,
    val dndActive: Boolean
) {
    val isMatch: Boolean
        get() = trueLabel == predictedLabel
}

data class DatasetSessionSummary(
    val fileName: String,
    val filePath: String,
    val runId: String,
    val trueLabel: ContextType,
    val requestedHz: Int,
    val repetition: Int,
    val windowCount: Int,
    val averageActualHz: Double,
    val matchCount: Int,
    val createdAtMs: Long
) {
    val accuracyPercent: Float
        get() = if (windowCount > 0) (matchCount * 100f / windowCount) else 0f
}

data class DatasetCoverage(
    val totalWindows: Int = 0,
    val targetTotalWindows: Int = 540,
    val deskWindows: Int = 0,
    val pocketWindows: Int = 0,
    val walkingWindows: Int = 0,
    val targetPerContext: Int = 180
)

data class ExportResult(
    val success: Boolean,
    val exportedFileCount: Int = 0,
    val destinationDescription: String = "",
    val primaryFilePath: String? = null,
    val errorMessage: String? = null
)
