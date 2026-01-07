package com.by.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.by.android.data.model.BuildResult
import com.by.android.data.model.JobStatus
import com.by.android.ui.theme.AndroidTheme

@Composable
fun StatusIcon(
    status: JobStatus,
    isBuilding: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val icon = when (status) {
        JobStatus.SUCCESS -> Icons.Default.CheckCircle
        JobStatus.FAILURE -> Icons.Default.Cancel
        JobStatus.UNSTABLE -> Icons.Default.Error
        JobStatus.DISABLED -> Icons.Default.PauseCircle
        JobStatus.ABORTED -> Icons.Default.StopCircle
        JobStatus.NOT_BUILT -> Icons.Default.RadioButtonUnchecked
        JobStatus.UNKNOWN -> Icons.AutoMirrored.Filled.Help
    }
    
    val alpha = if (isBuilding) {
        val infiniteTransition = rememberInfiniteTransition(label = "building")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        animatedAlpha
    } else {
        1f
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        modifier = modifier
            .size(size)
            .alpha(alpha),
        tint = status.statusColor
    )
}

@Composable
fun BuildStatusIcon(
    result: BuildResult,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val icon = when (result) {
        BuildResult.SUCCESS -> Icons.Default.CheckCircle
        BuildResult.FAILURE -> Icons.Default.Cancel
        BuildResult.UNSTABLE -> Icons.Default.Error
        BuildResult.ABORTED -> Icons.Default.StopCircle
        BuildResult.NOT_BUILT -> Icons.Default.RadioButtonUnchecked
        BuildResult.BUILDING -> Icons.Default.RadioButtonUnchecked
        BuildResult.UNKNOWN -> Icons.AutoMirrored.Filled.Help
    }
    
    val alpha = if (result == BuildResult.BUILDING) {
        val infiniteTransition = rememberInfiniteTransition(label = "building")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        animatedAlpha
    } else {
        1f
    }
    
    Icon(
        imageVector = icon,
        contentDescription = result.name,
        modifier = modifier
            .size(size)
            .alpha(alpha),
        tint = result.resultColor
    )
}

@Preview(showBackground = true)
@Composable
fun StatusIconPreview() {
    AndroidTheme {
        StatusIcon(status = JobStatus.SUCCESS, isBuilding = false)
    }
}

