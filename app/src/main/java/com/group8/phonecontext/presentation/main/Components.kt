package com.group8.phonecontext.presentation.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.presentation.theme.CrispOffWhite
import com.group8.phonecontext.presentation.theme.DeepObsidian
import com.group8.phonecontext.presentation.theme.DeepPurpleGlow
import com.group8.phonecontext.presentation.theme.ElectricViolet
import com.group8.phonecontext.presentation.theme.EmeraldPulse
import com.group8.phonecontext.presentation.theme.FrostedGlassTint
import com.group8.phonecontext.presentation.theme.MatteCharcoal
import com.group8.phonecontext.presentation.theme.MutedAsh
import com.group8.phonecontext.presentation.theme.NavBarTopBorder
import com.group8.phonecontext.presentation.theme.SlateGraphite
import com.group8.phonecontext.presentation.theme.SubtleRimBorder

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    borderColor: Color = SubtleRimBorder,
    useFrostedOverlay: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MatteCharcoal)
            .then(
                if (useFrostedOverlay) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(FrostedGlassTint, Color.Transparent)
                        )
                    )
                } else {
                    Modifier
                }
            )
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun PulsingStatusDot(
    color: Color = EmeraldPulse,
    size: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "sensor_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun ContextIconBadge(
    contextType: ContextType,
    isDeskFocusActive: Boolean,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (contextType) {
        ContextType.DESK_FACE_DOWN -> Icons.Rounded.StayCurrentLandscape
        ContextType.POCKET_SEATED -> Icons.Rounded.PhoneAndroid
        ContextType.WALKING_IN_HAND -> Icons.AutoMirrored.Rounded.DirectionsWalk
        ContextType.UNKNOWN -> Icons.Rounded.Radar
    }

    val containerBg = if (isDeskFocusActive || contextType == ContextType.DESK_FACE_DOWN) {
        ElectricViolet.copy(alpha = 0.20f)
    } else {
        SlateGraphite
    }
    val iconTint = if (isDeskFocusActive || contextType == ContextType.DESK_FACE_DOWN) {
        ElectricViolet
    } else {
        CrispOffWhite
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(96.dp)
            .then(
                if (contextType == ContextType.DESK_FACE_DOWN) {
                    Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    DeepPurpleGlow.copy(alpha = 0.34f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension * 0.62f
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(containerBg)
                .border(
                    width = 1.dp,
                    color = if (contextType == ContextType.DESK_FACE_DOWN) {
                        ElectricViolet.copy(alpha = 0.45f)
                    } else {
                        SubtleRimBorder
                    },
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contextType.displayName,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selectedOption: T,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitleOf: ((T) -> String)? = null
) {
    val trackShape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(SlateGraphite)
            .border(1.dp, SubtleRimBorder, trackShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val selected = option == selectedOption
            val itemShape = RoundedCornerShape(10.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(itemShape)
                    .background(if (selected) ElectricViolet else Color.Transparent)
                    .clickable(enabled = enabled) { onSelect(option) }
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = labelOf(option),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) CrispOffWhite else if (enabled) CrispOffWhite.copy(alpha = 0.85f) else MutedAsh
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (subtitleOf != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleOf(option),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = if (selected) CrispOffWhite.copy(alpha = 0.85f) else MutedAsh
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ElectricViolet,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = CrispOffWhite,
            disabledContainerColor = SlateGraphite,
            disabledContentColor = MutedAsh
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = CrispOffWhite
            )
        )
    }
}

@Composable
fun StatusPillBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color = Color.Transparent,
    leadingDotColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            if (leadingDotColor != null) {
                PulsingStatusDot(color = leadingDotColor, size = 8.dp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun ContextSenseBottomBar(
    currentTab: NavDestination,
    onSelectTab: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepObsidian)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NavBarTopBorder)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavDestination.entries.forEach { dest ->
                val selected = dest == currentTab
                val icon = when (dest) {
                    NavDestination.HOME -> Icons.Rounded.Home
                    NavDestination.EXPERIMENT -> Icons.Rounded.Science
                    NavDestination.LOGS -> Icons.AutoMirrored.Rounded.ListAlt
                    NavDestination.FOCUS -> Icons.Rounded.DoNotDisturbOn
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTab(dest) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) ElectricViolet.copy(alpha = 0.18f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = dest.label,
                            tint = if (selected) ElectricViolet else MutedAsh,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (selected) ElectricViolet else MutedAsh,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
