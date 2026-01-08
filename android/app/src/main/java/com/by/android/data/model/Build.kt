package com.by.android.data.model

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class Build(
    val number: Int,
    val url: String,
    val result: String? = null,
    val timestamp: Long? = null,
    val duration: Long? = null,
    val displayName: String? = null,
    val building: Boolean? = null,
    val description: String? = null,
    val estimatedDuration: Long? = null,
    val fullDisplayName: String? = null
) {
    val buildResult: BuildResult
        get() = when {
            result == null && building == true -> BuildResult.BUILDING
            result == "SUCCESS" -> BuildResult.SUCCESS
            result == "FAILURE" -> BuildResult.FAILURE
            result == "UNSTABLE" -> BuildResult.UNSTABLE
            result == "ABORTED" -> BuildResult.ABORTED
            result == "NOT_BUILT" -> BuildResult.NOT_BUILT
            else -> BuildResult.UNKNOWN
        }
    
    val startDate: Date?
        get() = timestamp?.let { Date(it) }
    
    val formattedDuration: String
        get() {
            val durationMs = duration ?: return "-"
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
            return when {
                seconds < 60 -> "${seconds}秒"
                seconds < 3600 -> {
                    val minutes = seconds / 60
                    val secs = seconds % 60
                    "${minutes}分${secs}秒"
                }
                else -> {
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    "${hours}小时${minutes}分"
                }
            }
        }
    
    val formattedTimestamp: String
        get() {
            val date = startDate ?: return "-"
            val now = System.currentTimeMillis()
            val diff = now - date.time
            
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            
            return when {
                seconds < 60 -> "刚刚"
                minutes < 60 -> "${minutes}分钟前"
                hours < 24 -> "${hours}小时前"
                days < 30 -> "${days}天前"
                days < 365 -> "${days / 30}个月前"
                else -> "${days / 365}年前"
            }
        }
    
    val detailedTimestamp: String
        get() {
            val date = startDate ?: return "-"
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            return formatter.format(date)
        }
}

enum class BuildResult {
    SUCCESS,
    FAILURE,
    UNSTABLE,
    ABORTED,
    NOT_BUILT,
    BUILDING,
    UNKNOWN;
    
    val resultColor: Color
        get() = when (this) {
            SUCCESS -> Color(0xFF4CAF50)
            FAILURE -> Color(0xFFF44336)
            UNSTABLE -> Color(0xFFFF9800)
            ABORTED -> Color(0xFF9E9E9E)
            NOT_BUILT -> Color(0xFF9E9E9E)
            BUILDING -> Color(0xFF2196F3)
            UNKNOWN -> Color(0xFF9E9E9E)
        }
    
    val displayName: String
        get() = when (this) {
            SUCCESS -> "成功"
            FAILURE -> "失败"
            UNSTABLE -> "不稳定"
            ABORTED -> "已中止"
            NOT_BUILT -> "未构建"
            BUILDING -> "构建中"
            UNKNOWN -> "未知"
        }
}

// API Response wrappers
data class BuildsResponse(
    val builds: List<Build>
)

data class JobDetailResponse(
    val name: String,
    val url: String,
    val color: String? = null,
    val builds: List<Build>? = null,
    val lastBuild: BuildReference? = null,
    val lastSuccessfulBuild: BuildReference? = null,
    val lastFailedBuild: BuildReference? = null,
    val description: String? = null,
    val buildable: Boolean? = null,
    val property: List<JobProperty>? = null
) {
    /** Get parameter definitions from job properties */
    val parameterDefinitions: List<ParameterDefinition>
        get() = property?.flatMap { it.parameterDefinitions ?: emptyList() } ?: emptyList()
    
    /** Check if this job has parameters */
    val hasParameters: Boolean
        get() = parameterDefinitions.isNotEmpty()
}

// Parameter Models

data class JobProperty(
    val parameterDefinitions: List<ParameterDefinition>? = null
)

data class ParameterDefinition(
    val name: String,
    val type: String? = null,
    val description: String? = null,
    val defaultParameterValue: ParameterValue? = null,
    val choices: List<String>? = null
) {
    /** Parameter type enum for easier handling */
    val parameterType: ParameterType
        get() = when {
            type?.contains("Choice") == true || type?.contains("ExtendedChoice") == true -> ParameterType.CHOICE
            type?.contains("Boolean") == true -> ParameterType.BOOLEAN
            type?.contains("Text") == true -> ParameterType.TEXT
            type?.contains("Password") == true -> ParameterType.PASSWORD
            else -> ParameterType.STRING
        }
    
    /** Default value as string */
    val defaultValue: String
        get() = defaultParameterValue?.value ?: ""
}

data class ParameterValue(
    val name: String? = null,
    val value: String? = null
)

// Build Detail Response (includes parameters used in the build)
data class BuildDetailResponse(
    val number: Int,
    val url: String,
    val result: String? = null,
    val timestamp: Long? = null,
    val duration: Long? = null,
    val displayName: String? = null,
    val building: Boolean? = null,
    val description: String? = null,
    val estimatedDuration: Long? = null,
    val fullDisplayName: String? = null,
    val actions: List<BuildAction>? = null
) {
    /** Get build parameters from actions */
    val buildParameters: List<BuildParameter>
        get() = actions?.firstOrNull { it.parameters != null }?.parameters ?: emptyList()
    
    /** Check if this build has parameters */
    val hasParameters: Boolean
        get() = buildParameters.isNotEmpty()
    
    /** Convert to Build model */
    fun toBuild(): Build = Build(
        number = number,
        url = url,
        result = result,
        timestamp = timestamp,
        duration = duration,
        displayName = displayName,
        building = building,
        description = description,
        estimatedDuration = estimatedDuration,
        fullDisplayName = fullDisplayName
    )
}

data class BuildAction(
    val parameters: List<BuildParameter>? = null
)

data class BuildParameter(
    val name: String,
    val value: String? = null
)

enum class ParameterType {
    STRING,
    TEXT,
    BOOLEAN,
    CHOICE,
    PASSWORD
}

