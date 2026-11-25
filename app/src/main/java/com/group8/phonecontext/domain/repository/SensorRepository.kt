package com.group8.phonecontext.domain.repository

import com.group8.phonecontext.domain.model.SamplingRateHz
import com.group8.phonecontext.domain.model.SensorAvailability
import com.group8.phonecontext.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun getSensorAvailability(): SensorAvailability
    fun observeSensorReadings(samplingRate: SamplingRateHz): Flow<SensorReading>
}
