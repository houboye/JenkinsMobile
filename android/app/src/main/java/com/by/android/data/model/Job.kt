package com.by.android.data.model

import androidx.compose.ui.graphics.Color

data class Job(
    val name: String,
    val url: String,
    val color: String? = null,
    val lastBuild: BuildReference? = null,
    val lastSuccessfulBuild: BuildReference? = null,
    val lastFailedBuild: BuildReference? = null,
    val lastCompletedBuild: BuildReference? = null,
    val buildable: Boolean? = null,
    val healthReport: List<HealthReport>? = null
) {
    val status: JobStatus
        get() = when (color) {
            "blue", "blue_anime" -> JobStatus.SUCCESS
            "red", "red_anime" -> JobStatus.FAILURE
            "yellow", "yellow_anime" -> JobStatus.UNSTABLE
            "grey", "grey_anime", "disabled", "disabled_anime" -> JobStatus.DISABLED
            "aborted", "aborted_anime" -> JobStatus.ABORTED
            "notbuilt", "notbuilt_anime" -> JobStatus.NOT_BUILT
            else -> JobStatus.UNKNOWN
        }
    
    val isBuilding: Boolean
        get() = color?.contains("_anime") == true
    
    val healthScore: Int
        get() = healthReport?.firstOrNull()?.score ?: 0
}

data class BuildReference(
    val number: Int,
    val url: String
)

data class HealthReport(
    val description: String? = null,
    val score: Int = 0
)

enum class JobStatus {
    SUCCESS,
    FAILURE,
    UNSTABLE,
    DISABLED,
    ABORTED,
    NOT_BUILT,
    UNKNOWN;
    
    val statusColor: Color
        get() = when (this) {
            SUCCESS -> Color(0xFF4CAF50)
            FAILURE -> Color(0xFFF44336)
            UNSTABLE -> Color(0xFFFF9800)
            DISABLED -> Color(0xFF9E9E9E)
            ABORTED -> Color(0xFF9E9E9E)
            NOT_BUILT -> Color(0xFF9E9E9E)
            UNKNOWN -> Color(0xFF9E9E9E)
        }
}

// API Response wrapper
data class JobsResponse(
    val jobs: List<Job>
)

