package com.group8.phonecontext.data.datasource

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.group8.phonecontext.domain.model.ProximityState
import com.group8.phonecontext.domain.model.SamplingRateHz
import com.group8.phonecontext.domain.model.SensorAvailability
import com.group8.phonecontext.domain.model.SensorReading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.min

/**
 * Wraps Android [SensorManager] and [SensorEventListener] to collect raw Accelerometer,
 * Ambient Light, and Proximity sensor streams.
 *
 * Accelerometer sampling rate is requested per-condition (5, 20, or 50 Hz) while Light and
 * Proximity listeners remain constant using readings as they arrive.
 */
class AndroidSensorDataSource(
    context: Context
) {
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    fun checkAvailability(): SensorAvailability = SensorAvailability(
        hasAccelerometer = accelerometer != null,
        hasLight = lightSensor != null,
        hasProximity = proximitySensor != null
    )

    fun observeReadings(samplingRate: SamplingRateHz): Flow<SensorReading> = callbackFlow {
        val accel = accelerometer
        if (accel == null) {
            close()
            return@callbackFlow
        }

        val proxMaxRange = proximitySensor?.maximumRange ?: 5f
        val nearThresholdCm = min(5f, if (proxMaxRange > 0f) proxMaxRange else 5f)

        val listener = object : SensorEventListener {
            @Volatile var latestLightLux = 0f
            @Volatile var latestProximityCm = proxMaxRange.coerceAtLeast(5f)
            @Volatile var latestProximityState = ProximityState.FAR

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_LIGHT -> {
                        if (event.values.isNotEmpty()) {
                            latestLightLux = event.values[0].coerceAtLeast(0f)
                        }
                    }

                    Sensor.TYPE_PROXIMITY -> {
                        if (event.values.isNotEmpty()) {
                            val cm = event.values[0].coerceAtLeast(0f)
                            latestProximityCm = cm
                            latestProximityState = if (cm < nearThresholdCm) {
                                ProximityState.NEAR
                            } else {
                                ProximityState.FAR
                            }
                        }
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        if (event.values.size >= 3) {
                            val reading = SensorReading(
                                wallTimeMs = System.currentTimeMillis(),
                                sensorTimestampNs = event.timestamp,
                                ax = event.values[0],
                                ay = event.values[1],
                                az = event.values[2],
                                lightLux = latestLightLux,
                                proximityCm = latestProximityCm,
                                proximityState = latestProximityState
                            )
                            trySend(reading)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        lightSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.registerListener(listener, accel, samplingRate.samplingPeriodUs)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
