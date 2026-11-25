package com.group8.phonecontext.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group8.phonecontext.presentation.theme.CrispOffWhite
import com.group8.phonecontext.presentation.theme.ElectricViolet
import com.group8.phonecontext.presentation.theme.EmeraldPulse
import com.group8.phonecontext.presentation.theme.MutedAsh
import com.group8.phonecontext.presentation.theme.SlateGraphite
import com.group8.phonecontext.presentation.theme.SubtleRimBorder

@Composable
fun ProjectInfoScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val datasetDone = uiState.datasetCoverage.totalWindows >= uiState.datasetCoverage.targetTotalWindows
    val focusLogsDone = uiState.recordedFocusSequences.size >= 3

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SlateGraphite)
                    .border(1.dp, SubtleRimBorder, CircleShape)
                    .clickable { onBack() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = CrispOffWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Project Info & About",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp)
                )
                Text(
                    text = "ContextSense • Group 8 Research Prototype",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Section 5.1: Project Identity Card
        GlassCard(useFrostedOverlay = true) {
            Text(
                text = "Project Identity",
                style = MaterialTheme.typography.titleMedium.copy(color = ElectricViolet, fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow("Title", "Phone Context Recognition with Automatic Focus Mode")
            InfoRow("Track", "Selected Option: C — Recognizing Phone Context from Sensors")
            InfoRow("Group", "Group 8")
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SubtleRimBorder)
            Text(
                text = "Team Members",
                style = MaterialTheme.typography.labelSmall.copy(color = MutedAsh)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• Jayasinghe U.H.N — Index: 220264H",
                style = MaterialTheme.typography.bodyMedium.copy(color = CrispOffWhite)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "• Ekanayaka L.A.D — Index: 220155B",
                style = MaterialTheme.typography.bodyMedium.copy(color = CrispOffWhite)
            )
        }

        // Section 5.2: Experimental Environment & Rule Summary Card
        GlassCard {
            Text(
                text = "Experimental Environment & Rule Pipeline",
                style = MaterialTheme.typography.titleMedium.copy(color = ElectricViolet, fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow("Test Device", "Infinix Note 40 (Android 16)")
            InfoRow("Sensors Used", "Accelerometer (TYPE_ACCELEROMETER), Ambient Light (TYPE_LIGHT), Proximity (TYPE_PROXIMITY)")
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SubtleRimBorder)
            Text(
                text = "Feature & Threshold Pipeline Summary",
                style = MaterialTheme.typography.labelSmall.copy(color = CrispOffWhite, fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "3.0s non-overlapping windows → Movement Variance + Mean Axis Acceleration (X, Y, Z) + Light (Lux) + Proximity (Near / Far).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Separates dark/stationary Face-down Desk (Z ≈ -9.8 m/s², low tilt, low variance) from dark/stationary In Pocket (tilted/vertical orientation, Proximity = Near, Lux ≈ 0) and Walking (high movement variance).",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Section 5.3: Study Scope & Deliverables Checklist
        GlassCard {
            Text(
                text = "Study Scope & Deliverables Checklist",
                style = MaterialTheme.typography.titleMedium.copy(color = ElectricViolet, fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateGraphite.copy(alpha = 0.7f))
                    .border(1.dp, SubtleRimBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Study Scope Note: Fixed single device, single user, consistent front trouser pocket position, and controlled indoor lighting.",
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 17.sp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DeliverableRow(
                completed = true,
                text = "Kotlin sensor recording & live prediction app"
            )
            DeliverableRow(
                completed = datasetDone,
                text = "540-window labeled CSV dataset (${uiState.datasetCoverage.totalWindows} / 540 captured)"
            )
            DeliverableRow(
                completed = false,
                text = "Confusion matrices & Python reproducibility script"
            )
            DeliverableRow(
                completed = focusLogsDone,
                text = "3× Face-down Focus Action activation/release logs at 20 Hz (${uiState.recordedFocusSequences.size} / 3 logged)"
            )
            DeliverableRow(
                completed = true,
                text = "README & AI-Usage attribution log"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(color = CrispOffWhite)
        )
    }
}

@Composable
private fun DeliverableRow(
    completed: Boolean,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Icon(
            imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (completed) EmeraldPulse else MutedAsh,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (completed) CrispOffWhite else MutedAsh
            )
        )
    }
}
