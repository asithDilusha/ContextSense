package com.group8.phonecontext.presentation.main

import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.domain.model.DatasetCoverage
import com.group8.phonecontext.domain.model.DatasetSessionSummary
import com.group8.phonecontext.domain.model.FocusSequenceLog
import com.group8.phonecontext.domain.model.LoggedWindowRecord
import com.group8.phonecontext.domain.model.ProximityState
import com.group8.phonecontext.domain.model.SamplingRateHz
import com.group8.phonecontext.domain.model.SensorAvailability
import com.group8.phonecontext.domain.model.SensorWindow

enum class NavDestination(val label: String) {
    HOME("Home"),
    EXPERIMENT("Experiment"),
    LOGS("Logs"),
    FOCUS("Focus")
}

enum class RecordingPhase {
    IDLE,
    SETTLING_COUNTDOWN,
    RECORDING_ACTIVE
}

enum class LogFilter(val label: String) {
    ALL_RATES("All Rates"),
    HZ_5("5 Hz"),
    HZ_20("20 Hz"),
    HZ_50("50 Hz"),
    MISMATCHES_ONLY("Mismatches Only")
}

data class ExperimentCondition(
    val context: ContextType,
    val rate: SamplingRateHz
)

data class MainUiState(
    // Navigation
    val currentTab: NavDestination = NavDestination.HOME,
    val showProjectInfo: Boolean = false,

    // Hardware & Live Sensor Telemetry
    val sensorAvailability: SensorAvailability = SensorAvailability(),
    val selectedRate: SamplingRateHz = SamplingRateHz.HZ_20,
    val actualDeliveredHz: Double = 0.0,
    val liveAx: Float = 0f,
    val liveAy: Float = 0f,
    val liveAz: Float = 0f,
    val liveLightLux: Float = 0f,
    val liveProximityCm: Float = 5f,
    val liveProximityState: ProximityState = ProximityState.FAR,

    // 3-Second Window & Live Classification
    val windowElapsedSec: Float = 0f,
    val latestWindow: SensorWindow? = null,
    val predictedContext: ContextType = ContextType.UNKNOWN,

    // Experiment Configuration & State Machine
    val selectedTrueLabel: ContextType = ContextType.DESK_FACE_DOWN,
    val selectedRepetition: Int = 1,
    val recordingPhase: RecordingPhase = RecordingPhase.IDLE,
    val settlingSecondsRemaining: Int = 5,
    val recordedWindowCount: Int = 0,
    val targetWindowsPerRun: Int = 20,
    val activeRunElapsedSec: Int = 0,
    val activeRunMatchCount: Int = 0,
    val discardedWindowsInRun: Int = 0,
    val currentWindowFlaggedDiscard: Boolean = false,
    val shuffledOrderLabel: String = "Tap Shuffle to randomize condition order",
    val shuffledQueue: List<ExperimentCondition> = emptyList(),
    val shuffledIndex: Int = 0,

    // Automatic Focus Mode (DND) & Settings
    val hasNotificationPolicyAccess: Boolean = false,
    val isAppFocusActive: Boolean = false,
    val isExternalDndActive: Boolean = false,
    val autoFocusEnabled: Boolean = true,
    val respectPreExistingDnd: Boolean = true,
    val keepScreenAwake: Boolean = true,

    // Focus Action Transition Test Logger (20 Hz)
    val isFocusTransitionTestRunning: Boolean = false,
    val activeFocusSequence: FocusSequenceLog? = null,
    val recordedFocusSequences: List<FocusSequenceLog> = emptyList(),

    // Logs & Datasets Viewer
    val selectedLogFilter: LogFilter = LogFilter.ALL_RATES,
    val datasetCoverage: DatasetCoverage = DatasetCoverage(),
    val sessions: List<DatasetSessionSummary> = emptyList(),
    val windowRecords: List<LoggedWindowRecord> = emptyList(),
    val logsDirectoryPath: String = "",

    // User Feedback Toast / Banner
    val statusBannerMessage: String? = null
) {
    val isRecordingOrSettling: Boolean
        get() = recordingPhase != RecordingPhase.IDLE

    val filteredWindowRecords: List<LoggedWindowRecord>
        get() = when (selectedLogFilter) {
            LogFilter.ALL_RATES -> windowRecords
            LogFilter.HZ_5 -> windowRecords.filter { it.requestedHz == 5 }
            LogFilter.HZ_20 -> windowRecords.filter { it.requestedHz == 20 }
            LogFilter.HZ_50 -> windowRecords.filter { it.requestedHz == 50 }
            LogFilter.MISMATCHES_ONLY -> windowRecords.filter { !it.isMatch }
        }
}
