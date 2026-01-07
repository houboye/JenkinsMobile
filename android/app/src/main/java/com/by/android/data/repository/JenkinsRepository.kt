package com.by.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.by.android.data.api.JenkinsApi
import com.by.android.data.model.Build
import com.by.android.data.model.JenkinsView
import com.by.android.data.model.Job
import com.by.android.data.model.JobDetailResponse
import com.by.android.data.model.Server
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jenkins_settings")

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

class JenkinsRepository(private val context: Context) {
    
    private var api: JenkinsApi? = null
    private var currentServer: Server? = null
    
    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val USERNAME = stringPreferencesKey("username")
        private val API_TOKEN = stringPreferencesKey("api_token")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }
    
    val serverFlow: Flow<Server> = context.dataStore.data.map { preferences ->
        Server(
            url = preferences[SERVER_URL] ?: "",
            username = preferences[USERNAME] ?: "",
            apiToken = preferences[API_TOKEN] ?: ""
        )
    }
    
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }
    
    suspend fun saveServer(server: Server) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = server.url
            preferences[USERNAME] = server.username
            preferences[API_TOKEN] = server.apiToken
        }
    }
    
    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = loggedIn
        }
    }
    
    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
        }
        api = null
        currentServer = null
    }
    
    suspend fun login(server: Server): ApiResult<Boolean> {
        return try {
            configureApi(server)
            val response = api!!.getServerInfo()
            if (response.isSuccessful) {
                saveServer(server)
                setLoggedIn(true)
                ApiResult.Success(true)
            } else {
                when (response.code()) {
                    401, 403 -> ApiResult.Error("认证失败，请检查用户名和API Token", response.code())
                    else -> ApiResult.Error("服务器错误: ${response.code()}", response.code())
                }
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误: ${e.message}")
        }
    }
    
    suspend fun restoreSession(): Boolean {
        val server = serverFlow.first()
        val isLoggedIn = isLoggedInFlow.first()
        if (isLoggedIn && server.isValid) {
            configureApi(server)
            return true
        }
        return false
    }
    
    private fun configureApi(server: Server) {
        if (currentServer == server && api != null) return
        
        currentServer = server
        
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", server.authHeader)
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(server.baseUrl + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(JenkinsApi::class.java)
    }
    
    suspend fun getViews(): ApiResult<List<JenkinsView>> {
        return try {
            val response = api?.getServerInfo()
                ?: return ApiResult.Error("未配置服务器")
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.views ?: emptyList())
            } else {
                handleError(response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误: ${e.message}")
        }
    }
    
    suspend fun getAllJobs(): ApiResult<List<Job>> {
        return try {
            val response = api?.getAllJobs()
                ?: return ApiResult.Error("未配置服务器")
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.jobs ?: emptyList())
            } else {
                handleError(response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误: ${e.message}")
        }
    }
    
    suspend fun getViewJobs(viewName: String): ApiResult<List<Job>> {
        return try {
            val response = api?.getViewJobs(viewName)
                ?: return ApiResult.Error("未配置服务器")
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.jobs ?: emptyList())
            } else {
                handleError(response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误: ${e.message}")
        }
    }
    
    suspend fun getJobDetail(jobName: String): ApiResult<JobDetailResponse> {
        return try {
            val response = api?.getJobDetail(jobName)
                ?: return ApiResult.Error("未配置服务器")
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("数据为空")
            } else {
                handleError(response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误: ${e.message}")
        }
    }
    
    suspend fun getBuild(jobName: String, buildNumber: Int): ApiResult<Build> {
        return try {
            val response = api?.getBuild(jobName, buildNumber)
                ?: return ApiResult.Error("未配置服务器")
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("数据为空")
            } else {
                handleError(response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误: ${e.message}")
        }
    }
    
    suspend fun triggerBuild(jobName: String): ApiResult<Boolean> {
        return try {
            val response = api?.triggerBuild(jobName)
                ?: return ApiResult.Error("未配置服务器")
            if (response.isSuccessful || response.code() == 201 || response.code() == 302) {
                ApiResult.Success(true)
            } else {
                handleError(response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误: ${e.message}")
        }
    }
    
    suspend fun getConsoleOutput(jobName: String, buildNumber: Int): ApiResult<String> {
        return try {
            val response = api?.getConsoleOutput(jobName, buildNumber)
                ?: return ApiResult.Error("未配置服务器")
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.string() ?: "")
            } else {
                handleError(response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error("网络错误: ${e.message}")
        }
    }
    
    private fun <T> handleError(code: Int): ApiResult<T> {
        return when (code) {
            401, 403 -> ApiResult.Error("认证失败", code)
            else -> ApiResult.Error("服务器错误: $code", code)
        }
    }
}

