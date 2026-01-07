package com.by.android.data.model

data class JenkinsView(
    val name: String,
    val url: String,
    val jobs: List<Job>? = null
) {
    val displayName: String
        get() = if (name.equals("all", ignoreCase = true)) "所有" else name
}

// API Response wrappers
data class JenkinsRootResponse(
    val views: List<JenkinsView>,
    val primaryView: JenkinsView? = null,
    val nodeDescription: String? = null,
    val nodeName: String? = null,
    val mode: String? = null
)

data class ViewDetailResponse(
    val name: String,
    val url: String,
    val jobs: List<Job>
)

