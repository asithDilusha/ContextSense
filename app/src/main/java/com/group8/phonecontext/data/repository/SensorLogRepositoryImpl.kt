package com.group8.phonecontext.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.domain.model.DatasetCoverage
import com.group8.phonecontext.domain.model.DatasetSessionSummary
import com.group8.phonecontext.domain.model.ExportResult
import com.group8.phonecontext.domain.model.FocusSequenceLog
import com.group8.phonecontext.domain.model.LoggedWindowRecord
import com.group8.phonecontext.domain.model.ProximityState
import com.group8.phonecontext.domain.model.SamplingRateHz
import com.group8.phonecontext.domain.model.SensorReading
import com.group8.phonecontext.domain.repository.ActiveLogSession
import com.group8.phonecontext.domain.repository.SensorLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles structured CSV persistence and scoped-storage Downloads export for the
 * ContextSense research dataset.
 *
 * Window CSV Header (standardized for Python analysis):
 * timestamp_ms,run_id,window_index,requested_hz,actual_hz,true_label,predicted_label,
 * accel_mean_x,accel_mean_y,accel_mean_z,movement_variance,light_lux,proximity_cm,
 * proximity_state,dnd_active
 */
class SensorLogRepositoryImpl(
    private val context: Context
) : SensorLogRepository {

    private val logsDir: File by lazy {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        File(base, "pcr_logs").apply { mkdirs() }
    }

    private val focusTransitionsFile: File by lazy {
        File(logsDir, "focus_transitions_20Hz.csv")
    }

    override fun getLogsDirectoryPath(): String = logsDir.absolutePath

    override suspend fun startSession(
        trueLabel: ContextType,
        requestedRate: SamplingRateHz,
        repetition: Int
    ): ActiveLogSession = withContext(Dispatchers.IO) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val labelToken = trueLabel.shortLabel.lowercase(Locale.US)
        val runId = "${labelToken}_${requestedRate.hz}Hz_run${repetition}_$stamp"

        val windowFileName = "context_${labelToken}_${requestedRate.hz}Hz_run${repetition}_$stamp.csv"
        val rawFileName = "raw_${labelToken}_${requestedRate.hz}Hz_run${repetition}_$stamp.csv"

        val windowFile = File(logsDir, windowFileName)
        val rawFile = File(logsDir, rawFileName)

        BufferedWriter(FileWriter(windowFile, false)).use { writer ->
            writer.write(WINDOW_CSV_HEADER)
            writer.newLine()
        }

        BufferedWriter(FileWriter(rawFile, false)).use { writer ->
            writer.write(RAW_CSV_HEADER)
            writer.newLine()
        }

        ActiveLogSession(
            runId = runId,
            windowCsvFileName = windowFileName,
            windowCsvPath = windowFile.absolutePath,
            rawCsvPath = rawFile.absolutePath
        )
    }

    override suspend fun appendRawReadings(
        session: ActiveLogSession,
        readings: List<SensorReading>,
        requestedHz: Int,
        windowIndex: Int,
        trueLabel: ContextType,
        predictedLabel: ContextType
    ) = withContext(Dispatchers.IO) {
        if (readings.isEmpty()) return@withContext
        val rawFile = File(session.rawCsvPath)
        BufferedWriter(FileWriter(rawFile, true)).use { writer ->
            for (r in readings) {
                writer.write(
                    String.format(
                        Locale.US,
                        "%d,%d,%s,%d,%d,%.5f,%.5f,%.5f,%.3f,%.3f,%s,%s,%s",
                        r.wallTimeMs,
                        r.sensorTimestampNs,
                        session.runId,
                        windowIndex,
                        requestedHz,
                        r.ax,
                        r.ay,
                        r.az,
                        r.lightLux,
                        r.proximityCm,
                        r.proximityState.csvValue,
                        trueLabel.csvValue,
                        predictedLabel.csvValue
                    )
                )
                writer.newLine()
            }
        }
    }

    override suspend fun appendWindowRecord(
        session: ActiveLogSession,
        record: LoggedWindowRecord
    ) = withContext(Dispatchers.IO) {
        val windowFile = File(session.windowCsvPath)
        BufferedWriter(FileWriter(windowFile, true)).use { writer ->
            writer.write(formatWindowRow(record))
            writer.newLine()
        }
    }

    override suspend fun closeSession(session: ActiveLogSession) {
        // Buffered writers flush and close per window so zero data is lost on sudden exit.
    }

    override suspend fun loadSessions(): List<DatasetSessionSummary> = withContext(Dispatchers.IO) {
        val files = logsDir.listFiles { file ->
            file.isFile && file.name.startsWith("context_") && file.name.endsWith(".csv")
        }?.sortedByDescending { it.lastModified() }.orEmpty()

        files.mapNotNull { file ->
            val records = parseWindowCsv(file)
            if (records.isEmpty()) return@mapNotNull null

            val first = records.first()
            val avgHz = records.map { it.actualHz }.average()
            val matchCount = records.count { it.isMatch }
            val rep = extractRepetitionFromName(file.name)

            DatasetSessionSummary(
                fileName = file.name,
                filePath = file.absolutePath,
                runId = first.runId,
                trueLabel = first.trueLabel,
                requestedHz = first.requestedHz,
                repetition = rep,
                windowCount = records.size,
                averageActualHz = avgHz,
                matchCount = matchCount,
                createdAtMs = file.lastModified()
            )
        }
    }

    override suspend fun loadAllWindowRecords(): List<LoggedWindowRecord> = withContext(Dispatchers.IO) {
        val files = logsDir.listFiles { file ->
            file.isFile && file.name.startsWith("context_") && file.name.endsWith(".csv")
        }?.sortedByDescending { it.lastModified() }.orEmpty()

        files.flatMap { parseWindowCsv(it) }
            .sortedByDescending { it.timestampMs }
    }

    override suspend fun computeDatasetCoverage(): DatasetCoverage = withContext(Dispatchers.IO) {
        val records = loadAllWindowRecords()
        DatasetCoverage(
            totalWindows = records.size,
            targetTotalWindows = 540,
            deskWindows = records.count { it.trueLabel == ContextType.DESK_FACE_DOWN },
            pocketWindows = records.count { it.trueLabel == ContextType.POCKET_SEATED },
            walkingWindows = records.count { it.trueLabel == ContextType.WALKING_IN_HAND },
            targetPerContext = 180
        )
    }

    override suspend fun exportAllSessionsToDownloads(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val allRecords = loadAllWindowRecords().sortedBy { it.timestampMs }
            if (allRecords.isEmpty()) {
                return@withContext ExportResult(
                    success = false,
                    errorMessage = "No recorded windows available to export yet."
                )
            }

            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val mergedFileName = "contextsense_dataset_$stamp.csv"
            val csvContent = buildString {
                appendLine(WINDOW_CSV_HEADER)
                for (record in allRecords) {
                    appendLine(formatWindowRow(record))
                }
            }

            // Always keep a copy in app external logs dir for FileProvider sharing / adb pull
            val localExportFile = File(logsDir, mergedFileName)
            localExportFile.writeText(csvContent)

            var exportedCount = 1
            var destination = localExportFile.absolutePath

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, mergedFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/ContextSense"
                    )
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(csvContent.toByteArray(Charsets.UTF_8))
                    }
                    destination = "Downloads/ContextSense/$mergedFileName"
                }

                // Also export focus transition logs if present
                if (focusTransitionsFile.exists() && focusTransitionsFile.length() > 0) {
                    val focusExportName = "contextsense_focus_transitions_$stamp.csv"
                    val focusValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, focusExportName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            "${Environment.DIRECTORY_DOWNLOADS}/ContextSense"
                        )
                    }
                    val focusUri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        focusValues
                    )
                    if (focusUri != null) {
                        context.contentResolver.openOutputStream(focusUri)?.use { output ->
                            output.write(focusTransitionsFile.readBytes())
                        }
                        exportedCount++
                    }
                }
            }

            ExportResult(
                success = true,
                exportedFileCount = exportedCount,
                destinationDescription = destination,
                primaryFilePath = localExportFile.absolutePath
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                errorMessage = e.message ?: "Failed to export CSV dataset."
            )
        }
    }

    override suspend fun saveFocusSequenceLog(log: FocusSequenceLog) = withContext(Dispatchers.IO) {
        val writeHeader = !focusTransitionsFile.exists() || focusTransitionsFile.length() == 0L
        BufferedWriter(FileWriter(focusTransitionsFile, true)).use { writer ->
            if (writeHeader) {
                writer.write(FOCUS_CSV_HEADER)
                writer.newLine()
            }
            writer.write(
                listOf(
                    log.sequenceNumber,
                    log.startedAtMs,
                    log.endedAtMs ?: "",
                    log.deskDetectedAtMs ?: "",
                    log.dndActivatedAtMs ?: "",
                    log.deskExitedAtMs ?: "",
                    log.dndReleasedAtMs ?: "",
                    log.activationDelayMs ?: "",
                    log.releaseDelayMs ?: "",
                    log.unwantedActivations,
                    log.testNotificationsSent,
                    log.reachedWalking,
                    log.reachedDesk,
                    log.reachedPocket
                ).joinToString(",")
            )
            writer.newLine()
        }
    }

    override suspend fun loadFocusSequenceLogs(): List<FocusSequenceLog> = withContext(Dispatchers.IO) {
        if (!focusTransitionsFile.exists()) return@withContext emptyList()
        focusTransitionsFile.readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val p = line.split(",")
                if (p.size < 14) return@mapNotNull null
                try {
                    FocusSequenceLog(
                        sequenceNumber = p[0].toInt(),
                        startedAtMs = p[1].toLong(),
                        endedAtMs = p[2].toLongOrNull(),
                        deskDetectedAtMs = p[3].toLongOrNull(),
                        dndActivatedAtMs = p[4].toLongOrNull(),
                        deskExitedAtMs = p[5].toLongOrNull(),
                        dndReleasedAtMs = p[6].toLongOrNull(),
                        activationDelayMs = p[7].toLongOrNull(),
                        releaseDelayMs = p[8].toLongOrNull(),
                        unwantedActivations = p[9].toIntOrNull() ?: 0,
                        testNotificationsSent = p[10].toIntOrNull() ?: 0,
                        reachedWalking = p[11].toBoolean(),
                        reachedDesk = p[12].toBoolean(),
                        reachedPocket = p[13].toBoolean()
                    )
                } catch (_: Exception) {
                    null
                }
            }
    }

    private fun parseWindowCsv(file: File): List<LoggedWindowRecord> {
        return try {
            file.readLines()
                .drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val cols = line.split(",")
                    if (cols.size < 15) return@mapNotNull null
                    try {
                        LoggedWindowRecord(
                            timestampMs = cols[0].toLong(),
                            runId = cols[1],
                            windowIndex = cols[2].toInt(),
                            requestedHz = cols[3].toInt(),
                            actualHz = cols[4].toDouble(),
                            trueLabel = ContextType.fromCsvValue(cols[5]),
                            predictedLabel = ContextType.fromCsvValue(cols[6]),
                            accelMeanX = cols[7].toFloat(),
                            accelMeanY = cols[8].toFloat(),
                            accelMeanZ = cols[9].toFloat(),
                            movementVariance = cols[10].toDouble(),
                            lightLux = cols[11].toFloat(),
                            proximityCm = cols[12].toFloat(),
                            proximityState = ProximityState.fromCsvValue(cols[13]),
                            dndActive = cols[14].toBoolean()
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun formatWindowRow(r: LoggedWindowRecord): String = String.format(
        Locale.US,
        "%d,%s,%d,%d,%.2f,%s,%s,%.4f,%.4f,%.4f,%.5f,%.2f,%.2f,%s,%b",
        r.timestampMs,
        r.runId,
        r.windowIndex,
        r.requestedHz,
        r.actualHz,
        r.trueLabel.csvValue,
        r.predictedLabel.csvValue,
        r.accelMeanX,
        r.accelMeanY,
        r.accelMeanZ,
        r.movementVariance,
        r.lightLux,
        r.proximityCm,
        r.proximityState.csvValue,
        r.dndActive
    )

    private fun extractRepetitionFromName(fileName: String): Int {
        val match = Regex("_run(\\d+)_").find(fileName)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
    }

    companion object {
        const val WINDOW_CSV_HEADER =
            "timestamp_ms,run_id,window_index,requested_hz,actual_hz,true_label,predicted_label," +
                "accel_mean_x,accel_mean_y,accel_mean_z,movement_variance,light_lux,proximity_cm," +
                "proximity_state,dnd_active"

        const val RAW_CSV_HEADER =
            "wall_time_ms,sensor_timestamp_ns,run_id,window_index,requested_hz,ax,ay,az," +
                "light_lux,proximity_cm,proximity_state,true_label,predicted_label"

        const val FOCUS_CSV_HEADER =
            "sequence_number,started_at_ms,ended_at_ms,desk_detected_at_ms,dnd_activated_at_ms," +
                "desk_exited_at_ms,dnd_released_at_ms,activation_delay_ms,release_delay_ms," +
                "unwanted_activations,test_notifications_sent,reached_walking,reached_desk,reached_pocket"
    }
}
