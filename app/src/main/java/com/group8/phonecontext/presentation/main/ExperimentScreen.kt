package com.group8.phonecontext.presentation.main

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.domain.model.SamplingRateHz
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
fun ExperimentScreen(
    uiState: MainUiState,
    onSelectTrueLabel: (ContextType) -> Unit,
    onSelectSamplingRate: (SamplingRateHz) -> Unit,
    onSelectRepetition: (Int) -> Unit,
    onShuffleOrder: () -> Unit,
    onStartRun: () -> Unit,
    onStopRun: () -> Unit,
    onFlagDiscardWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locked = uiState.isRecordingOrSettling

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 2.1: Screen Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Experiment Configuration",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure ground-truth label and accelerometer rate before recording",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Live Recording State Machine Card (Prominently shown when settling or active)
        if (uiState.isRecordingOrSettling) {
            ActiveRecordingStateCard(
                uiState = uiState,
                onFlagDiscardWindow = onFlagDiscardWindow
            )
        }

        // Section 2.2: Step 1 — True Context Label Selector
        GlassCard {
            Text(
                text = "1. Select Ground-Truth Label",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            SegmentedSelector(
                options = ContextType.experimentContexts,
                selectedOption = uiState.selectedTrueLabel,
                onSelect = onSelectTrueLabel,
                labelOf = { it.shortLabel },
                subtitleOf = {
                    when (it) {
                        ContextType.DESK_FACE_DOWN -> "Face-down"
                        ContextType.POCKET_SEATED -> "Seated"
                        ContextType.WALKING_IN_HAND -> "In hand"
                        ContextType.UNKNOWN -> ""
                    }
                },
                enabled = !locked
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Evaluation Safeguard Callout
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateGraphite.copy(alpha = 0.7f))
                    .border(1.dp, SubtleRimBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = ElectricViolet,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Selected label is saved to CSV strictly for accuracy evaluation and is never used by the prediction rules.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MutedAsh,
                        lineHeight = 17.sp
                    )
                )
            }
        }

        // Section 2.3: Step 2 — Accelerometer Sampling Rate Selector
        GlassCard {
            Text(
                text = "2. Accelerometer Sampling Frequency",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            SegmentedSelector(
                options = SamplingRateHz.entries,
                selectedOption = uiState.selectedRate,
                onSelect = onSelectSamplingRate,
                labelOf = { it.label },
                enabled = !locked && !uiState.isFocusTransitionTestRunning
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateGraphite.copy(alpha = 0.65f))
                    .border(1.dp, SubtleRimBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REQUESTED DELAY",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                    Text(
                        text = "${uiState.selectedRate.samplingPeriodUs} µs (${uiState.selectedRate.label})",
                        style = TabularBodyStyle
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MEASURED RATE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                    Text(
                        text = if (uiState.actualDeliveredHz > 0.0) {
                            String.format(Locale.US, "%.1f Hz", uiState.actualDeliveredHz)
                        } else {
                            "Measuring..."
                        },
                        style = TabularBodyStyle.copy(color = ElectricViolet)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android SensorManager treats requested rates as hints. Hardware event timestamps are recorded per sample.",
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Section 2.4: Step 3 — Run Repetition Tracker & Shuffle Order
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3. Run Repetition Tracker",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ElectricViolet.copy(alpha = 0.16f))
                        .border(1.dp, ElectricViolet.copy(alpha = 0.45f), CircleShape)
                        .clickable(enabled = !locked) { onShuffleOrder() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle condition order",
                        tint = ElectricViolet,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Shuffle Order",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricViolet,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SegmentedSelector(
                options = listOf(1, 2, 3),
                selectedOption = uiState.selectedRepetition,
                onSelect = onSelectRepetition,
                labelOf = { "Run $it / 3" },
                enabled = !locked
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = uiState.shuffledOrderLabel,
                style = MaterialTheme.typography.labelSmall.copy(color = MutedAsh)
            )
        }

        // Primary Action Button
        PrimaryPillButton(
            text = if (locked) "STOP DATA COLLECTION RUN" else "START 60S DATA COLLECTION RUN",
            onClick = if (locked) onStopRun else onStartRun,
            containerColor = if (locked) MutedCrimson else ElectricViolet,
            leadingIcon = if (locked) Icons.Rounded.Stop else Icons.Rounded.PlayArrow
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ActiveRecordingStateCard(
    uiState: MainUiState,
    onFlagDiscardWindow: () -> Unit
) {
    val isSettling = uiState.recordingPhase == RecordingPhase.SETTLING_COUNTDOWN
    val ringColor = if (isSettling) AmberAlert else ElectricViolet
    val progress = if (isSettling) {
        (5 - uiState.settlingSecondsRemaining) / 5f
    } else {
        (uiState.recordedWindowCount.toFloat() / uiState.targetWindowsPerRun.coerceAtLeast(1))
            .coerceIn(0f, 1f)
    }

    GlassCard(
        cornerRadius = 24.dp,
        borderColor = ringColor.copy(alpha = 0.55f),
        useFrostedOverlay = true,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusPillBadge(
                text = if (isSettling) "PHASE A — SETTLING BUFFER" else "PHASE B — ACTIVE RECORDING",
                containerColor = ringColor.copy(alpha = 0.18f),
                contentColor = ringColor,
                borderColor = ringColor.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0.05f, 1f) },
                    modifier = Modifier.size(132.dp),
                    color = ringColor,
                    trackColor = SlateGraphite,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isSettling) {
                            "${uiState.settlingSecondsRemaining}s"
                        } else {
                            "${uiState.recordedWindowCount}/${uiState.targetWindowsPerRun}"
                        },
                        style = TabularTitleStyle.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrispOffWhite
                        )
                    )
                    Text(
                        text = if (isSettling) "Settling" else "${uiState.activeRunElapsedSec}s / 60s",
                        style = TabularLabelStyle
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isSettling) {
                    "Get into position: ${uiState.settlingSecondsRemaining}s..."
                } else {
                    "Recording Window ${uiState.recordedWindowCount} / ${uiState.targetWindowsPerRun} (${uiState.activeRunElapsedSec}s / 60s)"
                },
                style = TabularBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isSettling) {
                Text(
                    text = "No windows are saved during this 5-second settling countdown.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    textAlign = TextAlign.Center
                )
            } else {
                val isMatch = uiState.predictedContext == uiState.selectedTrueLabel
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "True: ${uiState.selectedTrueLabel.shortLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MutedAsh)
                    )
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Pred: ${uiState.predictedContext.shortLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(color = CrispOffWhite)
                    )
                    StatusPillBadge(
                        text = if (isMatch) "MATCH" else "MISMATCH",
                        containerColor = if (isMatch) EmeraldPulse.copy(alpha = 0.2f) else AmberAlert.copy(alpha = 0.2f),
                        contentColor = if (isMatch) EmeraldPulse else AmberAlert
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onFlagDiscardWindow,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, AmberAlert.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = null,
                        tint = AmberAlert,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Flag / Discard Transition Window (${uiState.discardedWindowsInRun})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AmberAlert,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
