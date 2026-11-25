package com.group8.phonecontext.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group8.phonecontext.domain.model.DatasetCoverage
import com.group8.phonecontext.domain.model.DatasetSessionSummary
import com.group8.phonecontext.domain.model.LoggedWindowRecord
import com.group8.phonecontext.presentation.theme.AmberAlert
import com.group8.phonecontext.presentation.theme.CrispOffWhite
import com.group8.phonecontext.presentation.theme.ElectricViolet
import com.group8.phonecontext.presentation.theme.EmeraldPulse
import com.group8.phonecontext.presentation.theme.MutedAsh
import com.group8.phonecontext.presentation.theme.SlateGraphite
import com.group8.phonecontext.presentation.theme.SubtleRimBorder
import com.group8.phonecontext.presentation.theme.TabularBodyStyle
import com.group8.phonecontext.presentation.theme.TabularLabelStyle
import com.group8.phonecontext.presentation.theme.TabularTitleStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    uiState: MainUiState,
    onSelectFilter: (LogFilter) -> Unit,
    onExportAllCsv: () -> Unit,
    onShareSessionFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val filteredWindows = uiState.filteredWindowRecords

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 3.1: Screen Header & Export Trigger
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recorded Datasets",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(
                    onClick = onExportAllCsv,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, ElectricViolet),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Export All CSV",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricViolet,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // Section 3.2: Dataset Coverage Summary Card
        item {
            DatasetCoverageCard(coverage = uiState.datasetCoverage)
        }

        // Section 3.3: Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogFilter.entries.forEach { filter ->
                    val selected = filter == uiState.selectedLogFilter
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) ElectricViolet else SlateGraphite)
                            .border(
                                width = 1.dp,
                                color = if (selected) ElectricViolet else SubtleRimBorder,
                                shape = CircleShape
                            )
                            .clickable { onSelectFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (selected) CrispOffWhite else MutedAsh,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // CSV Session Files Section
        item {
            Text(
                text = "CSV Session Files (${uiState.sessions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
            )
        }

        if (uiState.sessions.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "No CSV sessions recorded yet.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Run a 60-second collection session from the Home or Experiment tab to generate labeled 3-second window datasets.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(uiState.sessions, key = { it.filePath }) { session ->
                SessionFileCard(
                    session = session,
                    onShareClick = { onShareSessionFile(session.filePath) }
                )
            }
        }

        // Section 3.4: Window-by-Window Telemetry Inspection List
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Window Inspection (${filteredWindows.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                )
                if (filteredWindows.isNotEmpty()) {
                    val acc = filteredWindows.count { it.isMatch } * 100f / filteredWindows.size
                    Text(
                        text = String.format(Locale.US, "Accuracy: %.1f%%", acc),
                        style = TabularLabelStyle.copy(color = EmeraldPulse, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        if (filteredWindows.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "No windows match the selected filter.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(
                items = filteredWindows.take(120),
                key = { "${it.runId}_${it.windowIndex}_${it.timestampMs}" }
            ) { record ->
                WindowInspectionCard(
                    record = record,
                    formattedTime = timeFormatter.format(Date(record.timestampMs))
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun DatasetCoverageCard(coverage: DatasetCoverage) {
    val progress = (coverage.totalWindows.toFloat() / coverage.targetTotalWindows.coerceAtLeast(1))
        .coerceIn(0f, 1f)

    GlassCard(useFrostedOverlay = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dataset Coverage",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
            )
            Text(
                text = "Total Captured: ${coverage.totalWindows} / ${coverage.targetTotalWindows} Windows",
                style = TabularLabelStyle.copy(color = ElectricViolet, fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape),
            color = ElectricViolet,
            trackColor = SlateGraphite,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoverageMiniCard(
                label = "Desk",
                count = coverage.deskWindows,
                target = coverage.targetPerContext,
                modifier = Modifier.weight(1f)
            )
            CoverageMiniCard(
                label = "Pocket",
                count = coverage.pocketWindows,
                target = coverage.targetPerContext,
                modifier = Modifier.weight(1f)
            )
            CoverageMiniCard(
                label = "Walking",
                count = coverage.walkingWindows,
                target = coverage.targetPerContext,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CoverageMiniCard(
    label: String,
    count: Int,
    target: Int,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SlateGraphite)
            .border(1.dp, SubtleRimBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MutedAsh)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$count / $target",
            style = TabularTitleStyle.copy(fontSize = 15.sp)
        )
        Text(
            text = "60 per rate",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
        )
    }
}

@Composable
private fun SessionFileCard(
    session: DatasetSessionSummary,
    onShareClick: () -> Unit
) {
    GlassCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.fileName,
                    style = TabularBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        "%d windows • Req: %d Hz • Actual: %.1f Hz • Acc: %.0f%%",
                        session.windowCount,
                        session.requestedHz,
                        session.averageActualHz,
                        session.accuracyPercent
                    ),
                    style = TabularLabelStyle
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SlateGraphite)
                    .border(1.dp, ElectricViolet.copy(alpha = 0.4f), CircleShape)
                    .clickable { onShareClick() }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share CSV session",
                    tint = ElectricViolet,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WindowInspectionCard(
    record: LoggedWindowRecord,
    formattedTime: String
) {
    val isMatch = record.isMatch
    GlassCard(contentPadding = PaddingValues(14.dp)) {
        // Row 1: Window ID, Timestamp, Rate badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "#%03d", record.windowIndex),
                    style = TabularBodyStyle.copy(color = ElectricViolet, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = formattedTime,
                    style = TabularLabelStyle
                )
            }
            StatusPillBadge(
                text = String.format(Locale.US, "%d Hz (%.1f)", record.requestedHz, record.actualHz),
                containerColor = SlateGraphite,
                contentColor = CrispOffWhite,
                borderColor = SubtleRimBorder
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: True vs Predicted + MATCH / MISMATCH pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "True: ${record.trueLabel.shortLabel}  vs.  Predicted: ${record.predictedLabel.shortLabel}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CrispOffWhite,
                    fontWeight = FontWeight.Medium
                )
            )
            StatusPillBadge(
                text = if (isMatch) "MATCH" else "MISMATCH",
                containerColor = if (isMatch) EmeraldPulse.copy(alpha = 0.18f) else AmberAlert.copy(alpha = 0.18f),
                contentColor = if (isMatch) EmeraldPulse else AmberAlert
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: Compact feature summary
        Text(
            text = String.format(
                Locale.US,
                "Var: %.3f  |  AvgZ: %.2f  |  Lux: %.1f  |  Prox: %s",
                record.movementVariance,
                record.accelMeanZ,
                record.lightLux,
                record.proximityState.shortLabel
            ),
            style = TabularLabelStyle.copy(color = MutedAsh)
        )
    }
}
