package com.group8.phonecontext.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.domain.model.FocusSequenceLog
import com.group8.phonecontext.domain.model.LoggedWindowRecord
import com.group8.phonecontext.domain.model.SamplingRateHz
import com.group8.phonecontext.domain.model.SensorReading
import com.group8.phonecontext.domain.repository.ActiveLogSession
import com.group8.phonecontext.domain.repository.FocusModeRepository
import com.group8.phonecontext.domain.repository.SensorLogRepository
import com.group8.phonecontext.domain.repository.SensorRepository
import com.group8.phonecontext.domain.usecase.FocusTransitionEvent
import com.group8.phonecontext.domain.usecase.ManageFocusModeUseCase
import com.group8.phonecontext.domain.usecase.ProcessSensorWindowUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val sensorRepository: SensorRepository,
    private val sensorLogRepository: SensorLogRepository,
    private val focusModeRepository: FocusModeRepository,
    private val processSensorWindowUseCase: ProcessSensorWindowUseCase,
    private val manageFocusModeUseCase: ManageFocusModeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            sensorAvailability = sensorRepository.getSensorAvailability(),
            hasNotificationPolicyAccess = focusModeRepository.hasNotificationPolicyAccess(),
            isAppFocusActive = focusModeRepository.isAppFocusActive(),
            isExternalDndActive = focusModeRepository.isExternalDndActive(),
            logsDirectoryPath = sensorLogRepository.getLogsDirectoryPath()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val windowBufferLock = Any()
    private val currentWindowReadings = mutableListOf<SensorReading>()
    private var currentWindowStartMs: Long = System.currentTimeMillis()
    private var liveWindowCounter: Int = 0
    private var lastUiTelemetryUpdateMs: Long = 0L

    private var sensorStreamJob: Job? = null
    private var windowTickerJob: Job? = null
    private var settlingCountdownJob: Job? = null
    private var activeLogSession: ActiveLogSession? = null

    init {
        refreshStoredData()
        startSensorPipeline(_uiState.value.selectedRate)
        startWindowTicker()
    }

    // region Navigation & Lifecycle

    fun selectTab(tab: NavDestination) {
        _uiState.update { it.copy(currentTab = tab, showProjectInfo = false) }
        if (tab == NavDestination.LOGS) {
            refreshStoredData()
        }
        if (tab == NavDestination.FOCUS) {
            refreshPermissions()
        }
    }

    fun openProjectInfo() {
        _uiState.update { it.copy(showProjectInfo = true) }
    }

    fun closeProjectInfo() {
        _uiState.update { it.copy(showProjectInfo = false) }
    }

    fun refreshPermissions() {
        _uiState.update {
            it.copy(
                hasNotificationPolicyAccess = focusModeRepository.hasNotificationPolicyAccess(),
                isAppFocusActive = focusModeRepository.isAppFocusActive(),
                isExternalDndActive = focusModeRepository.isExternalDndActive()
            )
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    // endregion

    // region Experiment Controls

    fun selectTrueLabel(label: ContextType) {
        if (_uiState.value.isRecordingOrSettling) return
        _uiState.update { it.copy(selectedTrueLabel = label) }
    }

    fun selectSamplingRate(rate: SamplingRateHz) {
        if (_uiState.value.isRecordingOrSettling || _uiState.value.isFocusTransitionTestRunning) return
        if (_uiState.value.selectedRate == rate) return
        _uiState.update { it.copy(selectedRate = rate, actualDeliveredHz = 0.0) }
        resetWindowBuffer()
        startSensorPipeline(rate)
    }

    fun selectRepetition(repetition: Int) {
        if (_uiState.value.isRecordingOrSettling) return
        _uiState.update { it.copy(selectedRepetition = repetition.coerceIn(1, 3)) }
    }

    fun shuffleConditionOrder() {
        if (_uiState.value.isRecordingOrSettling) return
        val currentQueue = _uiState.value.shuffledQueue
        val nextIndex = _uiState.value.shuffledIndex + 1

        val (queue, index) = if (currentQueue.isEmpty() || nextIndex >= currentQueue.size) {
            val fresh = ContextType.experimentContexts.flatMap { ctx ->
                SamplingRateHz.entries.map { rate -> ExperimentCondition(ctx, rate) }
            }.shuffled()
            fresh to 0
        } else {
            currentQueue to nextIndex
        }

        val chosen = queue[index]
        val rateChanged = chosen.rate != _uiState.value.selectedRate
        _uiState.update {
            it.copy(
                selectedTrueLabel = chosen.context,
                selectedRate = chosen.rate,
                shuffledQueue = queue,
                shuffledIndex = index,
                shuffledOrderLabel = "Shuffled ${index + 1}/${queue.size}: ${chosen.context.shortLabel} @ ${chosen.rate.label}"
            )
        }
        if (rateChanged) {
            resetWindowBuffer()
            startSensorPipeline(chosen.rate)
        }
    }

    fun startExperimentRun() {
        if (_uiState.value.isRecordingOrSettling) return
        if (_uiState.value.isFocusTransitionTestRunning) {
            stopFocusTransitionTest()
        }

        settlingCountdownJob?.cancel()
        _uiState.update {
            it.copy(
                recordingPhase = RecordingPhase.SETTLING_COUNTDOWN,
                settlingSecondsRemaining = SETTLING_SECONDS,
                recordedWindowCount = 0,
                activeRunElapsedSec = 0,
                activeRunMatchCount = 0,
                discardedWindowsInRun = 0,
                currentWindowFlaggedDiscard = false,
                statusBannerMessage = null
            )
        }

        settlingCountdownJob = viewModelScope.launch {
            for (sec in SETTLING_SECONDS downTo 1) {
                _uiState.update { it.copy(settlingSecondsRemaining = sec) }
                delay(1_000L)
            }

            val snapshot = _uiState.value
            val session = sensorLogRepository.startSession(
                trueLabel = snapshot.selectedTrueLabel,
                requestedRate = snapshot.selectedRate,
                repetition = snapshot.selectedRepetition
            )
            activeLogSession = session
            resetWindowBuffer()

            _uiState.update {
                it.copy(
                    recordingPhase = RecordingPhase.RECORDING_ACTIVE,
                    settlingSecondsRemaining = 0,
                    recordedWindowCount = 0,
                    activeRunElapsedSec = 0,
                    activeRunMatchCount = 0,
                    discardedWindowsInRun = 0,
                    currentWindowFlaggedDiscard = false
                )
            }
        }
    }

    fun stopExperimentRun(completedNormally: Boolean = false) {
        settlingCountdownJob?.cancel()
        settlingCountdownJob = null

        val sessionToClose = activeLogSession
        activeLogSession = null

        val count = _uiState.value.recordedWindowCount
        _uiState.update {
            it.copy(
                recordingPhase = RecordingPhase.IDLE,
                currentWindowFlaggedDiscard = false,
                statusBannerMessage = when {
                    completedNormally -> "Run complete: $count windows saved to ${sessionToClose?.windowCsvFileName.orEmpty()}"
                    sessionToClose != null -> "Run stopped early ($count windows saved to CSV)."
                    else -> "Settling countdown cancelled."
                }
            )
        }

        viewModelScope.launch {
            if (sessionToClose != null) {
                sensorLogRepository.closeSession(sessionToClose)
            }
            refreshStoredData()
        }
    }

    fun flagDiscardCurrentWindow() {
        if (_uiState.value.recordingPhase != RecordingPhase.RECORDING_ACTIVE) return
        resetWindowBuffer()
        _uiState.update {
            it.copy(
                discardedWindowsInRun = it.discardedWindowsInRun + 1,
                currentWindowFlaggedDiscard = true,
                statusBannerMessage = "Transition window discarded — capturing clean replacement window."
            )
        }
    }

    // endregion

    // region Logs & Export Controls

    fun selectLogFilter(filter: LogFilter) {
        _uiState.update { it.copy(selectedLogFilter = filter) }
    }

    fun exportAllCsv() {
        viewModelScope.launch {
            val result = sensorLogRepository.exportAllSessionsToDownloads()
            _uiState.update {
                it.copy(
                    statusBannerMessage = if (result.success) {
                        "Exported ${result.exportedFileCount} CSV file(s) to ${result.destinationDescription}"
                    } else {
                        result.errorMessage ?: "CSV export failed."
                    }
                )
            }
        }
    }

    fun refreshStoredData() {
        viewModelScope.launch {
            val sessions = sensorLogRepository.loadSessions()
            val windows = sensorLogRepository.loadAllWindowRecords()
            val coverage = sensorLogRepository.computeDatasetCoverage()
            val focusLogs = sensorLogRepository.loadFocusSequenceLogs()
            _uiState.update {
                it.copy(
                    sessions = sessions,
                    windowRecords = windows,
                    datasetCoverage = coverage,
                    recordedFocusSequences = focusLogs
                )
            }
        }
    }

    // endregion

    // region Focus Automation & Transition Test (20 Hz)

    fun setAutoFocusEnabled(enabled: Boolean) {
        _uiState.update { it.copy(autoFocusEnabled = enabled) }
        if (!enabled) {
            val decision = manageFocusModeUseCase.releaseIfOwned(System.currentTimeMillis())
            _uiState.update { it.copy(isAppFocusActive = decision.isAppFocusActive) }
        }
    }

    fun setRespectPreExistingDnd(enabled: Boolean) {
        _uiState.update { it.copy(respectPreExistingDnd = enabled) }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenAwake = enabled) }
    }

    fun startFocusTransitionTest() {
        if (_uiState.value.isFocusTransitionTestRunning) return
        if (_uiState.value.isRecordingOrSettling) {
            stopExperimentRun(completedNormally = false)
        }

        // Proposal mandates 20 Hz for the 3x Focus Action transition tests.
        val rateChanged = _uiState.value.selectedRate != SamplingRateHz.HZ_20
        val nextSeqNum = (_uiState.value.recordedFocusSequences.maxOfOrNull { it.sequenceNumber } ?: 0) + 1
        val seq = FocusSequenceLog(
            sequenceNumber = nextSeqNum,
            startedAtMs = System.currentTimeMillis()
        )

        _uiState.update {
            it.copy(
                selectedRate = SamplingRateHz.HZ_20,
                isFocusTransitionTestRunning = true,
                activeFocusSequence = seq,
                statusBannerMessage = "Focus Transition Sequence #$nextSeqNum started at 20 Hz (Walking → Desk → Pocket)."
            )
        }
        if (rateChanged) {
            resetWindowBuffer()
            startSensorPipeline(SamplingRateHz.HZ_20)
        }
    }

    fun stopFocusTransitionTest() {
        val active = _uiState.value.activeFocusSequence ?: return
        val finalized = active.copy(endedAtMs = System.currentTimeMillis())
        _uiState.update {
            it.copy(
                isFocusTransitionTestRunning = false,
                activeFocusSequence = null,
                statusBannerMessage = "Saved Focus Transition Sequence #${finalized.sequenceNumber} to CSV."
            )
        }
        viewModelScope.launch {
            sensorLogRepository.saveFocusSequenceLog(finalized)
            refreshStoredData()
        }
    }

    fun triggerTestNotification(): Boolean {
        val sent = focusModeRepository.sendTestNotification()
        _uiState.update { state ->
            val updatedSeq = if (sent && state.isFocusTransitionTestRunning && state.activeFocusSequence != null) {
                state.activeFocusSequence.copy(
                    testNotificationsSent = state.activeFocusSequence.testNotificationsSent + 1
                )
            } else {
                state.activeFocusSequence
            }
            state.copy(
                activeFocusSequence = updatedSeq,
                statusBannerMessage = if (sent) {
                    "Test notification dispatched — check whether DND suppressed sound/banner."
                } else {
                    "Enable notification permission to send test notifications."
                }
            )
        }
        return sent
    }

    // endregion

    // region Core Sensor & 3-Second Window Engine

    private fun startSensorPipeline(rate: SamplingRateHz) {
        sensorStreamJob?.cancel()
        sensorStreamJob = viewModelScope.launch {
            sensorRepository.observeSensorReadings(rate).collect { reading ->
                synchronized(windowBufferLock) {
                    currentWindowReadings.add(reading)
                }

                val now = reading.wallTimeMs
                if (now - lastUiTelemetryUpdateMs >= UI_TELEMETRY_THROTTLE_MS) {
                    lastUiTelemetryUpdateMs = now
                    _uiState.update {
                        it.copy(
                            liveAx = reading.ax,
                            liveAy = reading.ay,
                            liveAz = reading.az,
                            liveLightLux = reading.lightLux,
                            liveProximityCm = reading.proximityCm,
                            liveProximityState = reading.proximityState
                        )
                    }
                }
            }
        }
    }

    private fun startWindowTicker() {
        windowTickerJob?.cancel()
        currentWindowStartMs = System.currentTimeMillis()
        windowTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(WINDOW_TICK_MS)
                val now = System.currentTimeMillis()
                val elapsedMs = now - currentWindowStartMs
                val elapsedSec = (elapsedMs / 1000f).coerceIn(0f, WINDOW_DURATION_SEC)

                val currentState = _uiState.value
                val activeRunSec = if (currentState.recordingPhase == RecordingPhase.RECORDING_ACTIVE) {
                    (currentState.recordedWindowCount * 3 + elapsedSec.toInt()).coerceAtMost(60)
                } else {
                    currentState.activeRunElapsedSec
                }

                _uiState.update {
                    it.copy(
                        windowElapsedSec = elapsedSec,
                        activeRunElapsedSec = activeRunSec
                    )
                }

                if (elapsedMs >= WINDOW_DURATION_MS) {
                    finalizeCurrentWindow(windowStartMs = currentWindowStartMs, windowEndMs = now)
                }
            }
        }
    }

    private fun resetWindowBuffer() {
        synchronized(windowBufferLock) {
            currentWindowReadings.clear()
        }
        currentWindowStartMs = System.currentTimeMillis()
        _uiState.update { it.copy(windowElapsedSec = 0f) }
    }

    private suspend fun finalizeCurrentWindow(windowStartMs: Long, windowEndMs: Long) {
        val readingsSnapshot: List<SensorReading>
        synchronized(windowBufferLock) {
            readingsSnapshot = currentWindowReadings.toList()
            currentWindowReadings.clear()
        }
        currentWindowStartMs = windowEndMs

        val stateBefore = _uiState.value
        val nextIndex = ++liveWindowCounter

        // Pure-Kotlin feature extraction + deterministic rule classification (no trueLabel used!)
        val window = processSensorWindowUseCase(
            readings = readingsSnapshot,
            windowIndex = nextIndex,
            requestedRate = stateBefore.selectedRate,
            windowStartTimeMs = windowStartMs,
            windowEndTimeMs = windowEndMs
        )

        // Evaluate automatic DND Focus Mode after full 3-second window completes
        val focusDecision = manageFocusModeUseCase.evaluateWindow(
            predictedContext = window.predictedContext,
            autoFocusEnabled = stateBefore.autoFocusEnabled,
            respectExistingUserDnd = stateBefore.respectPreExistingDnd,
            nowMs = windowEndMs
        )

        // Update active Focus Transition Test metrics if running
        val updatedFocusSeq = updateFocusSequenceMetrics(
            current = stateBefore.activeFocusSequence,
            isRunning = stateBefore.isFocusTransitionTestRunning,
            predicted = window.predictedContext,
            event = focusDecision.event,
            nowMs = windowEndMs
        )

        _uiState.update {
            it.copy(
                windowElapsedSec = 0f,
                latestWindow = window,
                predictedContext = window.predictedContext,
                actualDeliveredHz = window.actualHz,
                hasNotificationPolicyAccess = focusModeRepository.hasNotificationPolicyAccess(),
                isAppFocusActive = focusDecision.isAppFocusActive,
                isExternalDndActive = focusModeRepository.isExternalDndActive(),
                activeFocusSequence = updatedFocusSeq,
                currentWindowFlaggedDiscard = false
            )
        }

        // Persist window & raw readings if in active 60-second recording phase
        val session = activeLogSession
        if (stateBefore.recordingPhase == RecordingPhase.RECORDING_ACTIVE && session != null) {
            val newWindowCount = stateBefore.recordedWindowCount + 1
            val record = LoggedWindowRecord(
                timestampMs = windowEndMs,
                runId = session.runId,
                windowIndex = newWindowCount,
                requestedHz = stateBefore.selectedRate.hz,
                actualHz = window.actualHz,
                trueLabel = stateBefore.selectedTrueLabel,
                predictedLabel = window.predictedContext,
                accelMeanX = window.accelMeanX,
                accelMeanY = window.accelMeanY,
                accelMeanZ = window.accelMeanZ,
                movementVariance = window.movementVariance,
                lightLux = window.meanLightLux,
                proximityCm = window.meanProximityCm,
                proximityState = window.proximityState,
                dndActive = focusDecision.isAppFocusActive
            )

            sensorLogRepository.appendWindowRecord(session, record)
            sensorLogRepository.appendRawReadings(
                session = session,
                readings = readingsSnapshot,
                requestedHz = stateBefore.selectedRate.hz,
                windowIndex = newWindowCount,
                trueLabel = stateBefore.selectedTrueLabel,
                predictedLabel = window.predictedContext
            )

            val updatedMatches = stateBefore.activeRunMatchCount + if (record.isMatch) 1 else 0
            _uiState.update {
                it.copy(
                    recordedWindowCount = newWindowCount,
                    activeRunMatchCount = updatedMatches,
                    activeRunElapsedSec = (newWindowCount * 3).coerceAtMost(60)
                )
            }

            if (newWindowCount >= stateBefore.targetWindowsPerRun) {
                stopExperimentRun(completedNormally = true)
            }
        }
    }

    private fun updateFocusSequenceMetrics(
        current: FocusSequenceLog?,
        isRunning: Boolean,
        predicted: ContextType,
        event: FocusTransitionEvent,
        nowMs: Long
    ): FocusSequenceLog? {
        if (!isRunning || current == null) return current
        var seq = current

        when (predicted) {
            ContextType.WALKING_IN_HAND -> seq = seq.copy(reachedWalking = true)
            ContextType.DESK_FACE_DOWN -> {
                seq = seq.copy(
                    reachedDesk = true,
                    deskDetectedAtMs = seq.deskDetectedAtMs ?: nowMs
                )
            }
            ContextType.POCKET_SEATED -> {
                seq = seq.copy(
                    reachedPocket = true,
                    deskExitedAtMs = if (seq.reachedDesk && seq.deskExitedAtMs == null) nowMs else seq.deskExitedAtMs
                )
            }
            ContextType.UNKNOWN -> Unit
        }

        if (predicted != ContextType.DESK_FACE_DOWN && seq.reachedDesk && seq.deskExitedAtMs == null) {
            seq = seq.copy(deskExitedAtMs = nowMs)
        }

        when (event) {
            FocusTransitionEvent.ACTIVATED -> {
                if (predicted != ContextType.DESK_FACE_DOWN) {
                    seq = seq.copy(unwantedActivations = seq.unwantedActivations + 1)
                } else if (seq.dndActivatedAtMs == null) {
                    val detectedAt = seq.deskDetectedAtMs ?: nowMs
                    seq = seq.copy(
                        dndActivatedAtMs = nowMs,
                        activationDelayMs = (nowMs - detectedAt).coerceAtLeast(0L)
                    )
                }
            }
            FocusTransitionEvent.RELEASED -> {
                if (seq.dndActivatedAtMs != null && seq.dndReleasedAtMs == null) {
                    val exitedAt = seq.deskExitedAtMs ?: nowMs
                    seq = seq.copy(
                        dndReleasedAtMs = nowMs,
                        releaseDelayMs = (nowMs - exitedAt).coerceAtLeast(0L)
                    )
                }
            }
            else -> Unit
        }

        return seq
    }

    override fun onCleared() {
        super.onCleared()
        settlingCountdownJob?.cancel()
        windowTickerJob?.cancel()
        sensorStreamJob?.cancel()
        manageFocusModeUseCase.releaseIfOwned(System.currentTimeMillis())
    }

    // endregion

    companion object {
        private const val WINDOW_DURATION_MS = 3_000L
        private const val WINDOW_DURATION_SEC = 3.0f
        private const val WINDOW_TICK_MS = 60L
        private const val UI_TELEMETRY_THROTTLE_MS = 80L
        private const val SETTLING_SECONDS = 5
    }
}
