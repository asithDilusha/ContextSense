package com.group8.phonecontext.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.presentation.theme.AmberAlert
import com.group8.phonecontext.presentation.theme.CrispOffWhite
import com.group8.phonecontext.presentation.theme.ElectricViolet
import com.group8.phonecontext.presentation.theme.EmeraldPulse
import com.group8.phonecontext.presentation.theme.MutedAsh
import com.group8.phonecontext.presentation.theme.MutedCrimson
import com.group8.phonecontext.presentation.theme.SlateGraphite
import com.group8.phonecontext.presentation.theme.SubtleRimBorder
import com.group8.phonecontext.presentation.theme.TabularBodyStyle
import com.group8.phonecontext.presentation.theme.TabularLabelStyle
import com.group8.phonecontext.presentation.theme.TabularTitleStyle
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onOpenProjectInfo: () -> Unit,
    onQuickStartOrStopRun: () -> Unit,
    onNavigateToExperiment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1.1: Top App Bar
        HomeTopBar(
            sensorsOk = uiState.sensorAvailability.allAvailable,
            rateLabel = uiState.selectedRate.label,
            onOpenProjectInfo = onOpenProjectInfo,
            onRateClick = onNavigateToExperiment
        )

        // Section 1.2: Hero Card — Live Context Prediction
        HeroPredictionCard(
            predictedContext = uiState.predictedContext,
            isFocusActive = uiState.isAppFocusActive,
            windowElapsedSec = uiState.windowElapsedSec,
            recordingPhase = uiState.recordingPhase,
            settlingSecondsRemaining = uiState.settlingSecondsRemaining,
            recordedWindowCount = uiState.recordedWindowCount,
            targetWindowsPerRun = uiState.targetWindowsPerRun
        )

        // Section 1.3: Live Telemetry Card (Real-Time Sensor Stream)
        LiveTelemetryCard(uiState = uiState)

        // Section 1.4: Quick Action Panel
        QuickActionPanel(
            isRunning = uiState.isRecordingOrSettling,
            selectedTrueLabel = uiState.selectedTrueLabel,
            selectedRateLabel = uiState.selectedRate.label,
            onQuickStartOrStopRun = onQuickStartOrStopRun
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HomeTopBar(
    sensorsOk: Boolean,
    rateLabel: String,
    onOpenProjectInfo: () -> Unit,
    onRateClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ContextSense",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Threshold Rule Engine v1.0",
                style = MaterialTheme.typography.labelSmall
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPillBadge(
                text = if (sensorsOk) "Sensors OK" else "Check Sensors",
                containerColor = SlateGraphite,
                contentColor = if (sensorsOk) CrispOffWhite else AmberAlert,
                borderColor = SubtleRimBorder,
                leadingDotColor = if (sensorsOk) EmeraldPulse else AmberAlert
            )

            StatusPillBadge(
                text = rateLabel,
                containerColor = SlateGraphite,
                contentColor = ElectricViolet,
                borderColor = ElectricViolet.copy(alpha = 0.35f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onRateClick() }
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SlateGraphite)
                    .border(1.dp, SubtleRimBorder, CircleShape)
                    .clickable { onOpenProjectInfo() }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Project Info & About",
                    tint = CrispOffWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroPredictionCard(
    predictedContext: ContextType,
    isFocusActive: Boolean,
    windowElapsedSec: Float,
    recordingPhase: RecordingPhase,
    settlingSecondsRemaining: Int,
    recordedWindowCount: Int,
    targetWindowsPerRun: Int
) {
    val isDeskDetected = predictedContext == ContextType.DESK_FACE_DOWN
    val borderColor = if (isFocusActive || isDeskDetected) {
        ElectricViolet.copy(alpha = 0.45f)
    } else {
        SubtleRimBorder
    }

    GlassCard(
        cornerRadius = 24.dp,
        borderColor = borderColor,
        useFrostedOverlay = true,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CURRENT PREDICTED CONTEXT",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    color = MutedAsh
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            ContextIconBadge(
                contextType = predictedContext,
                isDeskFocusActive = isFocusActive
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = predictedContext.displayName,
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // DND Status Banner inside Hero Card
            val bannerBg = if (isFocusActive) {
                ElectricViolet.copy(alpha = 0.18f)
            } else {
                SlateGraphite
            }
            val bannerBorder = if (isFocusActive) {
                ElectricViolet.copy(alpha = 0.55f)
            } else {
                SubtleRimBorder
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(bannerBg)
                    .border(1.dp, bannerBorder, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Icon(
                    imageVector = if (isFocusActive) Icons.Rounded.DoNotDisturbOn else Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = if (isFocusActive) ElectricViolet else MutedAsh,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFocusActive) {
                        "Focus Mode Active — Do Not Disturb Engaged"
                    } else {
                        "Monitoring Active — Normal Notifications"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isFocusActive) CrispOffWhite else MutedAsh,
                        fontWeight = if (isFocusActive) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )
            }

            if (recordingPhase != RecordingPhase.IDLE) {
                Spacer(modifier = Modifier.height(10.dp))
                val runText = if (recordingPhase == RecordingPhase.SETTLING_COUNTDOWN) {
                    "Settling into position: ${settlingSecondsRemaining}s..."
                } else {
                    "Recording CSV • Window $recordedWindowCount / $targetWindowsPerRun"
                }
                StatusPillBadge(
                    text = runText,
                    containerColor = if (recordingPhase == RecordingPhase.SETTLING_COUNTDOWN) {
                        AmberAlert.copy(alpha = 0.18f)
                    } else {
                        ElectricViolet.copy(alpha = 0.22f)
                    },
                    contentColor = if (recordingPhase == RecordingPhase.SETTLING_COUNTDOWN) {
                        AmberAlert
                    } else {
                        CrispOffWhite
                    },
                    borderColor = if (recordingPhase == RecordingPhase.SETTLING_COUNTDOWN) {
                        AmberAlert.copy(alpha = 0.45f)
                    } else {
                        ElectricViolet.copy(alpha = 0.45f)
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3-Second Window Progress Bar
            val progress = (windowElapsedSec / 3.0f).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3s Sliding Window",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = String.format(Locale.US, "Window: %.1fs / 3.0s", windowElapsedSec),
                    style = TabularLabelStyle.copy(color = CrispOffWhite)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = ElectricViolet,
                trackColor = SlateGraphite,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun LiveTelemetryCard(uiState: MainUiState) {
    val variance = uiState.latestWindow?.movementVariance ?: 0.0
    val varianceStatus = if (variance >= 0.85) "High Motion" else "Stationary"
    val varianceStatusColor = if (variance >= 0.85) AmberAlert else EmeraldPulse

    val lux = uiState.liveLightLux
    val luxStatus = if (lux < 10f) "Dark < 10 Lux" else "Lit Environment"

    val zAxis = uiState.liveAz
    val orientationLabel = when {
        zAxis <= -8.0f -> "Face-Down"
        zAxis >= 8.0f -> "Face-Up"
        else -> "Tilted / Upright"
    }

    GlassCard(
        cornerRadius = 18.dp,
        borderColor = Color(0x1AFFFFFF),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Sensor Readings (3s Window)",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
            )
            Text(
                text = if (uiState.actualDeliveredHz > 0.0) {
                    String.format(Locale.US, "Actual: %.1f Hz", uiState.actualDeliveredHz)
                } else {
                    "Actual: -- Hz"
                },
                style = TabularLabelStyle.copy(color = ElectricViolet, fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Metric Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryMetricTile(
                label = "MOVEMENT VARIANCE",
                value = String.format(Locale.US, "%.3f m²/s⁴", variance),
                subtext = varianceStatus,
                subtextColor = varianceStatusColor,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricTile(
                label = "AMBIENT LIGHT",
                value = String.format(Locale.US, "%.1f Lux", lux),
                subtext = luxStatus,
                subtextColor = if (lux < 10f) ElectricViolet else MutedAsh,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryMetricTile(
                label = "PROXIMITY SENSOR",
                value = uiState.liveProximityState.detailLabel,
                subtext = String.format(Locale.US, "%.1f cm", uiState.liveProximityCm),
                subtextColor = MutedAsh,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricTile(
                label = "SCREEN ORIENTATION",
                value = orientationLabel,
                subtext = String.format(Locale.US, "Z: %.2f m/s²", zAxis),
                subtextColor = if (zAxis <= -8.0f) ElectricViolet else MutedAsh,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Full-width Accelerometer Vector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateGraphite)
                .border(1.dp, SubtleRimBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(Locale.US, "X: %+.2f", uiState.liveAx),
                style = TabularBodyStyle
            )
            Text(
                text = String.format(Locale.US, "Y: %+.2f", uiState.liveAy),
                style = TabularBodyStyle
            )
            Text(
                text = String.format(Locale.US, "Z: %+.2f m/s²", uiState.liveAz),
                style = TabularBodyStyle.copy(color = ElectricViolet)
            )
        }
    }
}

@Composable
private fun TelemetryMetricTile(
    label: String,
    value: String,
    subtext: String,
    subtextColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SlateGraphite.copy(alpha = 0.65f))
            .border(1.dp, SubtleRimBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = TabularTitleStyle.copy(fontSize = 15.sp),
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtext,
            style = TabularLabelStyle.copy(color = subtextColor, fontSize = 11.sp),
            maxLines = 1
        )
    }
}

@Composable
private fun QuickActionPanel(
    isRunning: Boolean,
    selectedTrueLabel: ContextType,
    selectedRateLabel: String,
    onQuickStartOrStopRun: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PrimaryPillButton(
            text = if (isRunning) "STOP RUN" else "START 60S TEST RUN",
            onClick = onQuickStartOrStopRun,
            containerColor = if (isRunning) MutedCrimson else ElectricViolet,
            leadingIcon = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Includes 5-second settling countdown • Logs 20 windows to CSV",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Configured: ${selectedTrueLabel.shortLabel} @ $selectedRateLabel",
            style = MaterialTheme.typography.labelSmall.copy(color = ElectricViolet),
            textAlign = TextAlign.Center
        )
    }
}
