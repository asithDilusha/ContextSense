package com.group8.phonecontext.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.phonecontext.data.datasource.AndroidSensorDataSource
import com.group8.phonecontext.data.datasource.SystemFocusDataSource
import com.group8.phonecontext.data.repository.FocusModeRepositoryImpl
import com.group8.phonecontext.data.repository.SensorLogRepositoryImpl
import com.group8.phonecontext.data.repository.SensorRepositoryImpl
import com.group8.phonecontext.domain.usecase.ManageFocusModeUseCase
import com.group8.phonecontext.domain.usecase.ProcessSensorWindowUseCase
import com.group8.phonecontext.presentation.theme.ContextSenseTheme
import com.group8.phonecontext.presentation.theme.CrispOffWhite
import com.group8.phonecontext.presentation.theme.DeepObsidian
import com.group8.phonecontext.presentation.theme.ElectricViolet
import com.group8.phonecontext.presentation.theme.SlateGraphite
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.triggerTestNotification()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        // Manual Clean Architecture DI wiring
        val sensorDataSource = AndroidSensorDataSource(applicationContext)
        val focusDataSource = SystemFocusDataSource(applicationContext)

        val sensorRepository = SensorRepositoryImpl(sensorDataSource)
        val sensorLogRepository = SensorLogRepositoryImpl(applicationContext)
        val focusModeRepository = FocusModeRepositoryImpl(focusDataSource)

        val processSensorWindowUseCase = ProcessSensorWindowUseCase()
        val manageFocusModeUseCase = ManageFocusModeUseCase(focusModeRepository)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    sensorRepository = sensorRepository,
                    sensorLogRepository = sensorLogRepository,
                    focusModeRepository = focusModeRepository,
                    processSensorWindowUseCase = processSensorWindowUseCase,
                    manageFocusModeUseCase = manageFocusModeUseCase
                ) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            ContextSenseTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.keepScreenAwake) {
                    if (uiState.keepScreenAwake) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                ContextSenseApp(
                    uiState = uiState,
                    onSelectTab = viewModel::selectTab,
                    onOpenProjectInfo = viewModel::openProjectInfo,
                    onCloseProjectInfo = viewModel::closeProjectInfo,
                    onSelectTrueLabel = viewModel::selectTrueLabel,
                    onSelectSamplingRate = viewModel::selectSamplingRate,
                    onSelectRepetition = viewModel::selectRepetition,
                    onShuffleOrder = viewModel::shuffleConditionOrder,
                    onStartRun = viewModel::startExperimentRun,
                    onStopRun = { viewModel.stopExperimentRun(completedNormally = false) },
                    onFlagDiscardWindow = viewModel::flagDiscardCurrentWindow,
                    onSelectLogFilter = viewModel::selectLogFilter,
                    onExportAllCsv = viewModel::exportAllCsv,
                    onShareSessionFile = ::shareCsvFile,
                    onOpenDndSettings = ::openNotificationPolicySettings,
                    onToggleAutoFocus = viewModel::setAutoFocusEnabled,
                    onToggleRespectPreExistingDnd = viewModel::setRespectPreExistingDnd,
                    onToggleKeepScreenAwake = viewModel::setKeepScreenAwake,
                    onStartOrStopFocusTransitionTest = {
                        if (uiState.isFocusTransitionTestRunning) {
                            viewModel.stopFocusTransitionTest()
                        } else {
                            viewModel.startFocusTransitionTest()
                        }
                    },
                    onSendTestNotification = ::handleTestNotificationClick,
                    onDismissBanner = viewModel::dismissBanner
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refreshPermissions()
        }
    }

    private fun openNotificationPolicySettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun handleTestNotificationClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel.triggerTestNotification()
    }

    private fun shareCsvFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ContextSense Dataset: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share CSV Dataset"))
    }
}

@Composable
private fun ContextSenseApp(
    uiState: MainUiState,
    onSelectTab: (NavDestination) -> Unit,
    onOpenProjectInfo: () -> Unit,
    onCloseProjectInfo: () -> Unit,
    onSelectTrueLabel: (com.group8.phonecontext.domain.model.ContextType) -> Unit,
    onSelectSamplingRate: (com.group8.phonecontext.domain.model.SamplingRateHz) -> Unit,
    onSelectRepetition: (Int) -> Unit,
    onShuffleOrder: () -> Unit,
    onStartRun: () -> Unit,
    onStopRun: () -> Unit,
    onFlagDiscardWindow: () -> Unit,
    onSelectLogFilter: (LogFilter) -> Unit,
    onExportAllCsv: () -> Unit,
    onShareSessionFile: (String) -> Unit,
    onOpenDndSettings: () -> Unit,
    onToggleAutoFocus: (Boolean) -> Unit,
    onToggleRespectPreExistingDnd: (Boolean) -> Unit,
    onToggleKeepScreenAwake: (Boolean) -> Unit,
    onStartOrStopFocusTransitionTest: () -> Unit,
    onSendTestNotification: () -> Unit,
    onDismissBanner: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian),
        containerColor = DeepObsidian,
        bottomBar = {
            if (!uiState.showProjectInfo) {
                ContextSenseBottomBar(
                    currentTab = uiState.currentTab,
                    onSelectTab = onSelectTab
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepObsidian)
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (uiState.showProjectInfo) {
                ProjectInfoScreen(
                    uiState = uiState,
                    onBack = onCloseProjectInfo
                )
            } else {
                when (uiState.currentTab) {
                    NavDestination.HOME -> HomeScreen(
                        uiState = uiState,
                        onOpenProjectInfo = onOpenProjectInfo,
                        onQuickStartOrStopRun = {
                            if (uiState.isRecordingOrSettling) onStopRun() else onStartRun()
                        },
                        onNavigateToExperiment = { onSelectTab(NavDestination.EXPERIMENT) }
                    )

                    NavDestination.EXPERIMENT -> ExperimentScreen(
                        uiState = uiState,
                        onSelectTrueLabel = onSelectTrueLabel,
                        onSelectSamplingRate = onSelectSamplingRate,
                        onSelectRepetition = onSelectRepetition,
                        onShuffleOrder = onShuffleOrder,
                        onStartRun = onStartRun,
                        onStopRun = onStopRun,
                        onFlagDiscardWindow = onFlagDiscardWindow
                    )

                    NavDestination.LOGS -> LogsScreen(
                        uiState = uiState,
                        onSelectFilter = onSelectLogFilter,
                        onExportAllCsv = onExportAllCsv,
                        onShareSessionFile = onShareSessionFile
                    )

                    NavDestination.FOCUS -> FocusScreen(
                        uiState = uiState,
                        onOpenDndSettings = onOpenDndSettings,
                        onToggleAutoFocus = onToggleAutoFocus,
                        onToggleRespectPreExistingDnd = onToggleRespectPreExistingDnd,
                        onToggleKeepScreenAwake = onToggleKeepScreenAwake,
                        onStartOrStopFocusTransitionTest = onStartOrStopFocusTransitionTest,
                        onSendTestNotification = onSendTestNotification
                    )
                }
            }

            // Floating Status Toast Banner
            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateGraphite)
                        .border(1.dp, ElectricViolet.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .clickable { onDismissBanner() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.statusBannerMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(color = CrispOffWhite),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricViolet),
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}
