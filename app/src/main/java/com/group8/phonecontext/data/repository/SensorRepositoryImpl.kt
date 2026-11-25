package com.group8.phonecontext.data.repository

import com.group8.phonecontext.data.datasource.AndroidSensorDataSource
import com.group8.phonecontext.domain.model.SamplingRateHz
import com.group8.phonecontext.domain.model.SensorAvailability
import com.group8.phonecontext.domain.model.SensorReading
import com.group8.phonecontext.domain.repository.SensorRepository
import kotlinx.coroutines.flow.Flow

class SensorRepositoryImpl(
    private val sensorDataSource: AndroidSensorDataSource
) : SensorRepository {

    override fun getSensorAvailability(): SensorAvailability =
        sensorDataSource.checkAvailability()

    override fun observeSensorReadings(samplingRate: SamplingRateHz): Flow<SensorReading> =
        sensorDataSource.observeReadings(samplingRate)
}
