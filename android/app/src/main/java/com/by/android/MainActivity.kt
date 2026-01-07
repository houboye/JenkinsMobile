package com.by.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.by.android.ui.navigation.JenkinsNavGraph
import com.by.android.ui.navigation.Screen
import com.by.android.ui.theme.AndroidTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val app = application as JenkinsApp
        val repository = app.repository
        
        // Check if user is already logged in
        val isLoggedIn = runBlocking { repository.restoreSession() }
        
        setContent {
            AndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (isLoggedIn) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Login.route
                    }
                    
                    JenkinsNavGraph(
                        repository = repository,
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
