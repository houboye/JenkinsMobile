package com.by.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.by.android.data.model.Job
import com.by.android.data.repository.JenkinsRepository
import com.by.android.ui.builddetail.BuildDetailScreen
import com.by.android.ui.builddetail.BuildDetailViewModel
import com.by.android.ui.dashboard.DashboardScreen
import com.by.android.ui.dashboard.DashboardViewModel
import com.by.android.ui.jobdetail.JobDetailScreen
import com.by.android.ui.jobdetail.JobDetailViewModel
import com.by.android.ui.login.LoginScreen
import com.by.android.ui.login.LoginViewModel
import com.by.android.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object JobDetail : Screen("job_detail/{jobName}/{jobUrl}/{jobColor}") {
        fun createRoute(job: Job): String {
            val encodedName = java.net.URLEncoder.encode(job.name, "UTF-8")
            val encodedUrl = java.net.URLEncoder.encode(job.url, "UTF-8")
            val color = job.color ?: "unknown"
            return "job_detail/$encodedName/$encodedUrl/$color"
        }
    }
    object BuildDetail : Screen("build_detail/{jobName}/{jobUrl}/{buildNumber}/{buildUrl}") {
        fun createRoute(jobName: String, jobUrl: String, buildNumber: Int, buildUrl: String): String {
            val encodedJobName = java.net.URLEncoder.encode(jobName, "UTF-8")
            val encodedJobUrl = java.net.URLEncoder.encode(jobUrl, "UTF-8")
            val encodedBuildUrl = java.net.URLEncoder.encode(buildUrl, "UTF-8")
            return "build_detail/$encodedJobName/$encodedJobUrl/$buildNumber/$encodedBuildUrl"
        }
    }
    object Settings : Screen("settings")
}

@Composable
fun JenkinsNavGraph(
    repository: JenkinsRepository,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    val isLoggedIn by repository.isLoggedInFlow.collectAsState(initial = false)
    
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && navController.currentDestination?.route == Screen.Login.route) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        } else if (!isLoggedIn && navController.currentDestination?.route != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val viewModel = remember { LoginViewModel(repository) }
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            val viewModel = remember { DashboardViewModel(repository) }
            DashboardScreen(
                viewModel = viewModel,
                onJobClick = { job ->
                    navController.navigate(Screen.JobDetail.createRoute(job))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(
                navArgument("jobName") { type = NavType.StringType },
                navArgument("jobUrl") { type = NavType.StringType },
                navArgument("jobColor") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("jobName") ?: "",
                "UTF-8"
            )
            val jobUrl = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("jobUrl") ?: "",
                "UTF-8"
            )
            val jobColor = backStackEntry.arguments?.getString("jobColor")
            
            val job = Job(
                name = jobName,
                url = jobUrl,
                color = if (jobColor == "unknown") null else jobColor
            )
            
            val viewModel = remember(jobName, jobUrl) { JobDetailViewModel(repository, jobName, jobUrl) }
            
            JobDetailScreen(
                job = job,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onBuildClick = { build ->
                    navController.navigate(Screen.BuildDetail.createRoute(jobName, jobUrl, build.number, build.url))
                }
            )
        }
        
        composable(
            route = Screen.BuildDetail.route,
            arguments = listOf(
                navArgument("jobName") { type = NavType.StringType },
                navArgument("jobUrl") { type = NavType.StringType },
                navArgument("buildNumber") { type = NavType.IntType },
                navArgument("buildUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("jobName") ?: "",
                "UTF-8"
            )
            val jobUrl = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("jobUrl") ?: "",
                "UTF-8"
            )
            val buildNumber = backStackEntry.arguments?.getInt("buildNumber") ?: 0
            val buildUrl = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("buildUrl") ?: "",
                "UTF-8"
            )
            
            val viewModel = remember(jobName, buildNumber) { 
                BuildDetailViewModel(repository, jobName, buildNumber, buildUrl, jobUrl) 
            }
            
            BuildDetailScreen(
                jobName = jobName,
                buildNumber = buildNumber,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                serverFlow = repository.serverFlow,
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    kotlinx.coroutines.runBlocking {
                        repository.logout()
                    }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
