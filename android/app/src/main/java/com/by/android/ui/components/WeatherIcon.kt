package com.by.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.by.android.ui.theme.AndroidTheme

@Composable
fun WeatherIcon(
    healthScore: Int,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val (icon, tint) = when {
        healthScore > 80 -> Icons.Default.WbSunny to Color(0xFFFFCA28)
        healthScore > 60 -> Icons.Default.WbCloudy to Color(0xFFFF9800)
        healthScore > 40 -> Icons.Default.Cloud to Color(0xFF9E9E9E)
        healthScore > 20 -> Icons.Default.WaterDrop to Color(0xFF78909C)
        else -> Icons.Default.Thunderstorm to Color(0xFF607D8B)
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Health: $healthScore%",
        modifier = modifier.size(size),
        tint = tint
    )
}

@Preview(showBackground = true)
@Composable
fun WeatherIconPreview() {
    AndroidTheme {
        WeatherIcon(healthScore = 80)
    }
}

