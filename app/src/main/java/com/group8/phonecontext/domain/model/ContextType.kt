package com.group8.phonecontext.domain.model

/**
 * Mutually exclusive phone usage contexts recognized by the rule-based engine.
 */
enum class ContextType(
    val displayName: String,
    val shortLabel: String,
    val experimentDescription: String,
    val csvValue: String
) {
    DESK_FACE_DOWN(
        displayName = "Face-down on Desk",
        shortLabel = "Desk",
        experimentDescription = "Face-down on desk",
        csvValue = "desk_face_down"
    ),
    POCKET_SEATED(
        displayName = "In Pocket",
        shortLabel = "Pocket",
        experimentDescription = "Front trouser pocket, sitting",
        csvValue = "in_pocket"
    ),
    WALKING_IN_HAND(
        displayName = "Walking",
        shortLabel = "Walking",
        experimentDescription = "Phone in hand, normal pace",
        csvValue = "walking"
    ),
    UNKNOWN(
        displayName = "Analyzing...",
        shortLabel = "Unknown",
        experimentDescription = "Unclassified / settling",
        csvValue = "unknown"
    );

    companion object {
        val experimentContexts: List<ContextType> = listOf(
            DESK_FACE_DOWN,
            POCKET_SEATED,
            WALKING_IN_HAND
        )

        fun fromCsvValue(value: String): ContextType {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull {
                it.csvValue == normalized ||
                    it.name.lowercase() == normalized ||
                    it.shortLabel.lowercase() == normalized
            } ?: UNKNOWN
        }
    }
}

/**
 * Requested accelerometer sampling rates evaluated in the study (5, 20, and 50 Hz).
 */
enum class SamplingRateHz(
    val hz: Int,
    val samplingPeriodUs: Int,
    val label: String
) {
    HZ_5(hz = 5, samplingPeriodUs = 200_000, label = "5 Hz"),
    HZ_20(hz = 20, samplingPeriodUs = 50_000, label = "20 Hz"),
    HZ_50(hz = 50, samplingPeriodUs = 20_000, label = "50 Hz");

    companion object {
        fun fromHz(hz: Int): SamplingRateHz = when {
            hz <= 10 -> HZ_5
            hz <= 35 -> HZ_20
            else -> HZ_50
        }
    }
}

enum class ProximityState(
    val csvValue: String,
    val shortLabel: String,
    val detailLabel: String
) {
    NEAR(csvValue = "NEAR", shortLabel = "Near", detailLabel = "Near / Covered"),
    FAR(csvValue = "FAR", shortLabel = "Far", detailLabel = "Far / Open");

    companion object {
        fun fromCsvValue(value: String): ProximityState =
            if (value.trim().equals("NEAR", ignoreCase = true)) NEAR else FAR
    }
}

/**
 * Deterministic thresholds shared across 5 Hz, 20 Hz, and 50 Hz sampling rates.
 * Tune using practice recordings before locking for the 540-window test dataset.
 */
data class ClassificationThresholds(
    val walkingVarianceThreshold: Double = 0.85,
    val deskMaxVariance: Double = 0.65,
    val deskMaxZ: Float = -8.0f,
    val deskMaxHorizontalAccel: Float = 4.2f,
    val darkLuxThreshold: Float = 15.0f
)

/**
 * Recording log for a single 20 Hz Focus Action Transition Test sequence
 * (Walking -> Face-down on Desk -> Seated Pocket).
 */
data class FocusSequenceLog(
    val sequenceNumber: Int,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val deskDetectedAtMs: Long? = null,
    val dndActivatedAtMs: Long? = null,
    val deskExitedAtMs: Long? = null,
    val dndReleasedAtMs: Long? = null,
    val activationDelayMs: Long? = null,
    val releaseDelayMs: Long? = null,
    val unwantedActivations: Int = 0,
    val testNotificationsSent: Int = 0,
    val reachedWalking: Boolean = false,
    val reachedDesk: Boolean = false,
    val reachedPocket: Boolean = false
)
