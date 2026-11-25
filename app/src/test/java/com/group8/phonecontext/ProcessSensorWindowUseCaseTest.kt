package com.group8.phonecontext

import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.domain.model.ProximityState
import com.group8.phonecontext.domain.model.SamplingRateHz
import com.group8.phonecontext.domain.model.SensorReading
import com.group8.phonecontext.domain.usecase.ProcessSensorWindowUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class ProcessSensorWindowUseCaseTest {

    private val useCase = ProcessSensorWindowUseCase()

    @Test
    fun `classifies stationary face-down dark window as DESK_FACE_DOWN`() {
        val readings = generateReadings(
            hz = 20,
            durationSec = 3,
            ax = { 0.08f },
            ay = { -0.05f },
            az = { -9.80f },
            lightLux = 1.2f,
            proximityCm = 0.0f,
            proximityState = ProximityState.NEAR
        )

        val window = useCase(
            readings = readings,
            windowIndex = 1,
            requestedRate = SamplingRateHz.HZ_20,
            windowStartTimeMs = 1_000L,
            windowEndTimeMs = 4_000L
        )

        assertEquals(ContextType.DESK_FACE_DOWN, window.predictedContext)
        assertEquals(60, window.sampleCount)
        assertTrue(window.actualHz in 19.0..21.0)
        assertTrue(window.movementVariance < 0.05)
        assertEquals(-9.80f, window.accelMeanZ, 0.02f)
    }

    @Test
    fun `does not classify stationary face-down dark window as DESK_FACE_DOWN when proximity is FAR`() {
        val readings = generateReadings(
            hz = 20,
            durationSec = 3,
            ax = { 0.08f },
            ay = { -0.05f },
            az = { -9.80f },
            lightLux = 1.2f,
            proximityCm = 5.0f,
            proximityState = ProximityState.FAR
        )

        val window = useCase(
            readings = readings,
            windowIndex = 1,
            requestedRate = SamplingRateHz.HZ_20,
            windowStartTimeMs = 1_000L,
            windowEndTimeMs = 4_000L
        )

        assertEquals(ContextType.UNKNOWN, window.predictedContext)
        assertEquals(ProximityState.FAR, window.proximityState)
    }

    @Test
    fun `does not classify stationary face-down bright window as DESK_FACE_DOWN even when proximity is NEAR`() {
        val readings = generateReadings(
            hz = 20,
            durationSec = 3,
            ax = { 0.08f },
            ay = { -0.05f },
            az = { -9.80f },
            lightLux = 120.0f,
            proximityCm = 0.0f,
            proximityState = ProximityState.NEAR
        )

        val window = useCase(
            readings = readings,
            windowIndex = 1,
            requestedRate = SamplingRateHz.HZ_20,
            windowStartTimeMs = 1_000L,
            windowEndTimeMs = 4_000L
        )

        assertEquals(ContextType.UNKNOWN, window.predictedContext)
        assertEquals(ProximityState.NEAR, window.proximityState)
    }

    @Test
    fun `separates stationary dark seated pocket from face-down desk using orientation`() {
        val readings = generateReadings(
            hz = 20,
            durationSec = 3,
            ax = { 2.4f },
            ay = { 9.2f },
            az = { -1.8f },
            lightLux = 0.0f,
            proximityCm = 0.0f,
            proximityState = ProximityState.NEAR
        )

        val window = useCase(
            readings = readings,
            windowIndex = 2,
            requestedRate = SamplingRateHz.HZ_20,
            windowStartTimeMs = 4_000L,
            windowEndTimeMs = 7_000L
        )

        assertEquals(ContextType.POCKET_SEATED, window.predictedContext)
        assertEquals(ProximityState.NEAR, window.proximityState)
    }

    @Test
    fun `classifies high movement variance as WALKING_IN_HAND across 5 20 and 50 Hz`() {
        for (rate in SamplingRateHz.entries) {
            val readings = generateReadings(
                hz = rate.hz,
                durationSec = 3,
                ax = { idx -> (2.5 * sin(idx * 0.9)).toFloat() },
                ay = { idx -> (8.8 + 3.2 * sin(idx * 1.1)).toFloat() },
                az = { idx -> (2.0 * sin(idx * 0.7)).toFloat() },
                lightLux = 250f,
                proximityCm = 5.0f,
                proximityState = ProximityState.FAR
            )

            val window = useCase(
                readings = readings,
                windowIndex = 3,
                requestedRate = rate,
                windowStartTimeMs = 7_000L,
                windowEndTimeMs = 10_000L
            )

            assertEquals("Failed at ${rate.label}", ContextType.WALKING_IN_HAND, window.predictedContext)
            assertTrue("Expected high variance at ${rate.label}", window.movementVariance >= 0.85)
        }
    }

    private fun generateReadings(
        hz: Int,
        durationSec: Int,
        ax: (Int) -> Float,
        ay: (Int) -> Float,
        az: (Int) -> Float,
        lightLux: Float,
        proximityCm: Float,
        proximityState: ProximityState
    ): List<SensorReading> {
        val total = hz * durationSec
        val stepNs = 1_000_000_000L / hz
        val stepMs = 1_000L / hz
        return List(total) { i ->
            SensorReading(
                wallTimeMs = 1_000L + i * stepMs,
                sensorTimestampNs = 10_000_000_000L + i * stepNs,
                ax = ax(i),
                ay = ay(i),
                az = az(i),
                lightLux = lightLux,
                proximityCm = proximityCm,
                proximityState = proximityState
            )
        }
    }
}
