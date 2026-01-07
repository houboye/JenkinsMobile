package com.by.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.by.android.data.model.BuildReference
import com.by.android.data.model.HealthReport
import com.by.android.data.model.Job
import com.by.android.ui.theme.AndroidTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun JobRow(
    job: Job,
    onClick: () -> Unit,
    onTriggerBuild: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isTriggeringBuild by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Icon
        StatusIcon(
            status = job.status,
            isBuilding = job.isBuilding,
            size = 20.dp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Weather Icon
        WeatherIcon(
            healthScore = job.healthScore,
            size = 18.dp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Job Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = job.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                job.lastBuild?.let { build ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "#${build.number}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                job.lastSuccessfulBuild?.let { build ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "成功 #${build.number}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp
                        )
                    }
                }
                
                job.lastFailedBuild?.let { build ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "失败 #${build.number}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        
        // Trigger Build Button
        IconButton(
            onClick = {
                if (!isTriggeringBuild) {
                    isTriggeringBuild = true
                    onTriggerBuild()
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(2000)
                        isTriggeringBuild = false
                    }
                }
            },
            enabled = !isTriggeringBuild && (job.buildable != false)
        ) {
            if (isTriggeringBuild) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "触发构建",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JobRowPreview() {
    AndroidTheme {
        JobRow(
            job = Job(
                name = "ios_archive_pipeline",
                url = "https://jenkins.example.com/job/ios_archive_pipeline/",
                color = "blue",
                lastBuild = BuildReference(number = 1318, url = ""),
                lastSuccessfulBuild = BuildReference(number = 1318, url = ""),
                lastFailedBuild = BuildReference(number = 1315, url = ""),
                buildable = true,
                healthReport = listOf(HealthReport(description = "Build stability", score = 80))
            ),
            onClick = {},
            onTriggerBuild = {}
        )
    }
}

