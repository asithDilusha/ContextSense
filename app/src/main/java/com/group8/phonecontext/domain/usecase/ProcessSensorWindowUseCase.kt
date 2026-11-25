package com.group8.phonecontext.domain.usecase

import com.group8.phonecontext.domain.model.ClassificationThresholds
import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.domain.model.ProximityState
import com.group8.phonecontext.domain.model.SamplingRateHz
import com.group8.phonecontext.domain.model.SensorReading
import com.group8.phonecontext.domain.model.SensorWindow
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Pure-Kotlin feature extraction & deterministic rule-based classifier for a 3-second window.
 *
 * Notice that this use case NEVER receives the user-selected ground-truth label; labels are
 * strictly attached afterward at the logging layer for fair accuracy evaluation.
 */
class ProcessSensorWindowUseCase(
    private val thresholds: ClassificationThresholds = ClassificationThresholds()
) {

    operator fun invoke(
        readings: List<SensorReading>,
        windowIndex: Int,
        requestedRate: SamplingRateHz,
        windowStartTimeMs: Long,
        windowEndTimeMs: Long
    ): SensorWindow {
        if (readings.isEmpty()) {
            return SensorWindow(
                windowIndex = windowIndex,
                startTimeMs = windowStartTimeMs,
                endTimeMs = windowEndTimeMs,
                sampleCount = 0,
                requestedHz = requestedRate,
                actualHz = 0.0,
                accelMeanX = 0f,
                accelMeanY = 0f,
                accelMeanZ = 0f,
                movementVariance = 0.0,
                meanLightLux = 0f,
                meanProximityCm = 5f,
                proximityState = ProximityState.FAR,
                predictedContext = ContextType.UNKNOWN
            )
        }

        val count = readings.size
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0
        var sumLux = 0.0
        var sumProxCm = 0.0
        var nearCount = 0

        val magnitudes = DoubleArray(count)
        var sumMag = 0.0

        for (i in 0 until count) {
            val r = readings[i]
            sumX += r.ax
            sumY += r.ay
            sumZ += r.az
            sumLux += r.lightLux
            sumProxCm += r.proximityCm
            if (r.proximityState == ProximityState.NEAR) {
                nearCount++
            }
            val mag = sqrt((r.ax * r.ax + r.ay * r.ay + r.az * r.az).toDouble())
            magnitudes[i] = mag
            sumMag += mag
        }

        val meanX = (sumX / count).toFloat()
        val meanY = (sumY / count).toFloat()
        val meanZ = (sumZ / count).toFloat()
        val meanMag = sumMag / count

        var sumSqDiff = 0.0
        for (i in 0 until count) {
            val diff = magnitudes[i] - meanMag
            sumSqDiff += diff * diff
        }
        val movementVariance = sumSqDiff / count

        val meanLightLux = (sumLux / count).toFloat()
        val meanProximityCm = (sumProxCm / count).toFloat()
        val proximityState = if (nearCount * 2 >= count) ProximityState.NEAR else ProximityState.FAR

        val actualHz = computeActualHz(readings, windowStartTimeMs, windowEndTimeMs)

        val predicted = classify(
            meanX = meanX,
            meanY = meanY,
            meanZ = meanZ,
            movementVariance = movementVariance,
            meanLightLux = meanLightLux,
            proximityState = proximityState
        )

        return SensorWindow(
            windowIndex = windowIndex,
            startTimeMs = windowStartTimeMs,
            endTimeMs = windowEndTimeMs,
            sampleCount = count,
            requestedHz = requestedRate,
            actualHz = actualHz,
            accelMeanX = meanX,
            accelMeanY = meanY,
            accelMeanZ = meanZ,
            movementVariance = movementVariance,
            meanLightLux = meanLightLux,
            meanProximityCm = meanProximityCm,
            proximityState = proximityState,
            predictedContext = predicted
        )
    }

    private fun computeActualHz(
        readings: List<SensorReading>,
        windowStartTimeMs: Long,
        windowEndTimeMs: Long
    ): Double {
        if (readings.size >= 2) {
            val deltaNs = readings.last().sensorTimestampNs - readings.first().sensorTimestampNs
            if (deltaNs > 0L) {
                return (readings.size - 1).toDouble() / (deltaNs.toDouble() / 1_000_000_000.0)
            }
        }
        val durationSec = ((windowEndTimeMs - windowStartTimeMs).coerceAtLeast(1L)) / 1000.0
        return readings.size.toDouble() / durationSec
    }

    private fun classify(
        meanX: Float,
        meanY: Float,
        meanZ: Float,
        movementVariance: Double,
        meanLightLux: Float,
        proximityState: ProximityState
    ): ContextType {
        // 1. Walking produces sustained hand/step acceleration variance.
        if (movementVariance >= thresholds.walkingVarianceThreshold) {
            return ContextType.WALKING_IN_HAND
        }

        val horizontalAccel = hypot(meanX.toDouble(), meanY.toDouble()).toFloat()
        val isDark = meanLightLux <= thresholds.darkLuxThreshold
        val isCovered = proximityState == ProximityState.NEAR
        val isDarkAndCovered = isDark && isCovered
        val isDarkOrCovered = isDark || isCovered

        // 2. Face-down on desk: stationary, gravity predominantly along -Z (~ -9.81 m/s^2),
        //    minimal horizontal tilt, and screen both dark and covered (proximity NEAR).
        val isFlatFaceDown = meanZ <= thresholds.deskMaxZ &&
            horizontalAccel <= thresholds.deskMaxHorizontalAccel
        if (movementVariance <= thresholds.deskMaxVariance && isFlatFaceDown && isDarkAndCovered) {
            return ContextType.DESK_FACE_DOWN
        }

        // 3. Inside pocket while sitting: dark/covered and stationary/low motion, but oriented
        //    tilted or vertical rather than flat face-down on a horizontal desk.
        if (isDarkAndCovered && !isFlatFaceDown) {
            return ContextType.POCKET_SEATED
        }

        // 4. Fallback when stationary in open light (e.g. phone held upright while inspecting UI):
        //    classify as WALKING_IN_HAND only if movement exceeds walking threshold; otherwise
        //    keep UNKNOWN so Focus Mode DND never activates unintentionally.
        return ContextType.UNKNOWN
    }
}
