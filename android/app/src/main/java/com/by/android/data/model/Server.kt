package com.by.android.data.model

import android.util.Base64

data class Server(
    val url: String = "",
    val username: String = "",
    val apiToken: String = ""
) {
    val isValid: Boolean
        get() = url.isNotBlank() && username.isNotBlank() && apiToken.isNotBlank()
    
    val baseUrl: String
        get() {
            var urlString = url.trim()
            if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                urlString = "https://$urlString"
            }
            if (urlString.endsWith("/")) {
                urlString = urlString.dropLast(1)
            }
            return urlString
        }
    
    val authHeader: String
        get() {
            val credentials = "$username:$apiToken"
            val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            return "Basic $encoded"
        }
    
    companion object {
        val Empty = Server()
    }
}

