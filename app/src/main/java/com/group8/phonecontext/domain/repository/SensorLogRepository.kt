package com.group8.phonecontext.domain.repository

import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.domain.model.DatasetCoverage
import com.group8.phonecontext.domain.model.DatasetSessionSummary
import com.group8.phonecontext.domain.model.ExportResult
import com.group8.phonecontext.domain.model.FocusSequenceLog
import com.group8.phonecontext.domain.model.LoggedWindowRecord
import com.group8.phonecontext.domain.model.SamplingRateHz
import com.group8.phonecontext.domain.model.SensorReading

data class ActiveLogSession(
    val runId: String,
    val windowCsvFileName: String,
    val windowCsvPath: String,
    val rawCsvPath: String
)

interface SensorLogRepository {
    suspend fun startSession(
        trueLabel: ContextType,
        requestedRate: SamplingRateHz,
        repetition: Int
    ): ActiveLogSession

    suspend fun appendRawReadings(
        session: ActiveLogSession,
        readings: List<SensorReading>,
        requestedHz: Int,
        windowIndex: Int,
        trueLabel: ContextType,
        predictedLabel: ContextType
    )

    suspend fun appendWindowRecord(
        session: ActiveLogSession,
        record: LoggedWindowRecord
    )

    suspend fun closeSession(session: ActiveLogSession)

    suspend fun loadSessions(): List<DatasetSessionSummary>

    suspend fun loadAllWindowRecords(): List<LoggedWindowRecord>

    suspend fun computeDatasetCoverage(): DatasetCoverage

    suspend fun exportAllSessionsToDownloads(): ExportResult

    suspend fun saveFocusSequenceLog(log: FocusSequenceLog)

    suspend fun loadFocusSequenceLogs(): List<FocusSequenceLog>

    fun getLogsDirectoryPath(): String
}
