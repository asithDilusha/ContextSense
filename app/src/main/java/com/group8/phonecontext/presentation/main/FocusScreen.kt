package com.group8.phonecontext.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group8.phonecontext.domain.model.FocusSequenceLog
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen(
    uiState: MainUiState,
    onOpenDndSettings: () -> Unit,
    onToggleAutoFocus: (Boolean) -> Unit,
    onToggleRespectPreExistingDnd: (Boolean) -> Unit,
    onToggleKeepScreenAwake: (Boolean) -> Unit,
    onStartOrStopFocusTransitionTest: () -> Unit,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 4.1: Screen Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Focus Automation (DND)",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage automatic Do Not Disturb rules triggered by face-down desk detection",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Section 4.2: System Permission Status Card
        GlassCard(
            borderColor = if (uiState.hasNotificationPolicyAccess) {
                EmeraldPulse.copy(alpha = 0.4f)
            } else {
                AmberAlert.copy(alpha = 0.55f)
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = if (uiState.hasNotificationPolicyAccess) EmeraldPulse else AmberAlert,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Notification Policy Access",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                        )
                        Text(
                            text = if (uiState.hasNotificationPolicyAccess) {
                                "App-owned DND rule ready"
                            } else {
                                "Required to toggle AutomaticZenRule DND"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                StatusPillBadge(
                    text = if (uiState.hasNotificationPolicyAccess) "Granted" else "Action Required",
                    containerColor = if (uiState.hasNotificationPolicyAccess) {
                        EmeraldPulse.copy(alpha = 0.18f)
                    } else {
                        AmberAlert.copy(alpha = 0.18f)
                    },
                    contentColor = if (uiState.hasNotificationPolicyAccess) EmeraldPulse else AmberAlert
                )
            }

            if (!uiState.hasNotificationPolicyAccess) {
                Spacer(modifier = Modifier.height(14.dp))
                PrimaryPillButton(
                    text = "OPEN ANDROID DND SETTINGS",
                    onClick = onOpenDndSettings,
                    containerColor = ElectricViolet
                )
            }
        }

        // Section 4.3: Automation Behavior Toggles
        GlassCard {
            Text(
                text = "Automation Behavior",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            BehaviorToggleRow(
                title = "Automatic Desk Focus Mode",
                description = "Automatically enable DND after one complete 3-second window is classified as stationary and face-down.",
                checked = uiState.autoFocusEnabled,
                onCheckedChange = onToggleAutoFocus
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = SubtleRimBorder
            )

            BehaviorToggleRow(
                title = "Respect Pre-Existing User DND",
                description = "If DND was already enabled manually by the user before placing the phone down, do not disable it when leaving the desk context.",
                checked = uiState.respectPreExistingDnd,
                onCheckedChange = onToggleRespectPreExistingDnd
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = SubtleRimBorder
            )

            BehaviorToggleRow(
                title = "Keep Screen Awake During Active Session",
                description = "Prevents sleep mode while running foreground sensor collection (v1 requirement).",
                checked = uiState.keepScreenAwake,
                onCheckedChange = onToggleKeepScreenAwake
            )
        }

        // Section 4.4: Focus Action Transition Test Logger (20 Hz Evaluation)
        GlassCard(useFrostedOverlay = true) {
            Text(
                text = "Focus Action Transition Test (20 Hz)",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Run 3 sequences: Walking → Face-down on Desk → Seated Pocket at 20 Hz to log DND activation & release latency.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.isFocusTransitionTestRunning && uiState.activeFocusSequence != null) {
                val active = uiState.activeFocusSequence
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateGraphite)
                        .border(1.dp, ElectricViolet.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recording Sequence #${active.sequenceNumber} (Live: ${uiState.predictedContext.displayName})",
                        style = TabularBodyStyle.copy(color = ElectricViolet, fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SequenceStepPill("1. Walking", active.reachedWalking, Modifier.weight(1f))
                        SequenceStepPill("2. Desk", active.reachedDesk, Modifier.weight(1f))
                        SequenceStepPill("3. Pocket", active.reachedPocket, Modifier.weight(1f))
                    }
                    Text(
                        text = "DND Active: ${if (uiState.isAppFocusActive) "ENGAGED" else "IDLE"} • Test Notifications Sent: ${active.testNotificationsSent}",
                        style = TabularLabelStyle
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            PrimaryPillButton(
                text = if (uiState.isFocusTransitionTestRunning) {
                    "FINISH & SAVE SEQUENCE"
                } else {
                    "START FOCUS TRANSITION TEST (20 HZ)"
                },
                onClick = onStartOrStopFocusTransitionTest,
                containerColor = if (uiState.isFocusTransitionTestRunning) MutedCrimson else ElectricViolet,
                leadingIcon = if (uiState.isFocusTransitionTestRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSendTestNotification,
                shape = CircleShape,
                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.65f)),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = ElectricViolet,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Test Notification",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = CrispOffWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // Recorded Transition Log List
        Text(
            text = "Recorded Transition Logs (${uiState.recordedFocusSequences.size} / 3)",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
        )

        if (uiState.recordedFocusSequences.isEmpty()) {
            GlassCard {
                Text(
                    text = "No focus transition sequences recorded yet. Tap \"Start Focus Transition Test (20 Hz)\" above to log Sequences 1–3.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            uiState.recordedFocusSequences.forEach { seq ->
                FocusSequenceCard(seq = seq, timeFormatter = timeFormatter)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BehaviorToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CrispOffWhite,
                checkedTrackColor = ElectricViolet,
                uncheckedThumbColor = MutedAsh,
                uncheckedTrackColor = SlateGraphite,
                uncheckedBorderColor = SubtleRimBorder
            )
        )
    }
}

@Composable
private fun SequenceStepPill(
    label: String,
    reached: Boolean,
    modifier: Modifier = Modifier
) {
    StatusPillBadge(
        text = label,
        containerColor = if (reached) EmeraldPulse.copy(alpha = 0.2f) else SlateGraphite,
        contentColor = if (reached) EmeraldPulse else MutedAsh,
        borderColor = if (reached) EmeraldPulse.copy(alpha = 0.45f) else SubtleRimBorder,
        modifier = modifier
    )
}

@Composable
private fun FocusSequenceCard(
    seq: FocusSequenceLog,
    timeFormatter: SimpleDateFormat
) {
    val activatedStr = seq.dndActivatedAtMs?.let { timeFormatter.format(Date(it)) } ?: "Not triggered"
    val releasedStr = seq.dndReleasedAtMs?.let { timeFormatter.format(Date(it)) } ?: "Not released"
    val actDelayStr = seq.activationDelayMs?.let { "${it} ms" } ?: "--"
    val relDelayStr = seq.releaseDelayMs?.let { "${it} ms" } ?: "--"

    GlassCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sequence ${seq.sequenceNumber}",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, color = ElectricViolet)
            )
            StatusPillBadge(
                text = "Unwanted Activations: ${seq.unwantedActivations}",
                containerColor = if (seq.unwantedActivations == 0) {
                    EmeraldPulse.copy(alpha = 0.16f)
                } else {
                    AmberAlert.copy(alpha = 0.18f)
                },
                contentColor = if (seq.unwantedActivations == 0) EmeraldPulse else AmberAlert
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "DND Activated At: $activatedStr   •   Delay: $actDelayStr",
            style = TabularLabelStyle.copy(color = CrispOffWhite)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "DND Released At: $releasedStr   •   Delay: $relDelayStr",
            style = TabularLabelStyle
        )
    }
}
