package com.by.android.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.by.android.data.api.JenkinsApi
import com.by.android.data.model.Build
import com.by.android.data.model.CrumbResponse
import com.by.android.data.model.JenkinsView
import com.by.android.data.model.Job
import com.by.android.data.model.JobDetailResponse
import com.by.android.data.model.Server
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private var httpClient: OkHttpClient? = null
    private var currentServer: Server? = null
    private var cachedCrumb: CrumbResponse? = null
    private val gson = Gson()
    
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
        httpClient = null
        currentServer = null
        cachedCrumb = null
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
        cachedCrumb = null
        
        // Cookie 存储 - Jenkins crumb 需要和 session 绑定
        val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore.getOrPut(url.host) { mutableListOf() }.apply {
                    // 移除同名 cookie，添加新的
                    cookies.forEach { newCookie ->
                        removeAll { it.name == newCookie.name }
                        add(newCookie)
                    }
                }
            }
            
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        }
        
        // 统一使用一个带认证的 OkHttpClient
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", server.authHeader)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)  // 添加 cookie 管理
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        httpClient = client
        
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
    
    suspend fun getJobDetailByUrl(jobUrl: String): ApiResult<JobDetailResponse> {
        return try {
            val fullUrl = buildJobApiUrl(
                jobUrl = jobUrl,
                suffix = "api/json",
                query = "tree=name,url,color,description,buildable,builds[number,url,result,timestamp,duration,displayName,building,description],lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url],property[parameterDefinitions[name,type,description,defaultParameterValue[name,value],choices]]"
            )
            
            val response = api?.getJobDetailByUrl(fullUrl)
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
    
    /**
     * Trigger build using direct OkHttp call (matching iOS implementation)
     */
    suspend fun triggerBuild(jobName: String, parameters: Map<String, String>? = null): ApiResult<Boolean> {
        val server = currentServer ?: return ApiResult.Error("未配置服务器")
        val fullUrl = "${server.baseUrl}/job/${Uri.encode(jobName)}/buildWithParameters"
        return executeTriggerBuild(fullUrl, parameters)
    }
    
    /**
     * Trigger build by URL using direct OkHttp call (matching iOS implementation)
     */
    suspend fun triggerBuildByUrl(jobUrl: String, parameters: Map<String, String>? = null): ApiResult<Boolean> {
        val fullUrl = buildJobApiUrl(jobUrl = jobUrl, suffix = "buildWithParameters")
        return executeTriggerBuild(fullUrl, parameters)
    }
    
    /**
     * Execute build trigger request (shared implementation)
     */
    private suspend fun executeTriggerBuild(url: String, parameters: Map<String, String>?): ApiResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val server = currentServer ?: return@withContext ApiResult.Error("未配置服务器")
                val client = httpClient ?: return@withContext ApiResult.Error("未配置服务器")
                
                android.util.Log.d("JenkinsRepo", "=== triggerBuild ===")
                android.util.Log.d("JenkinsRepo", "url: $url")
                android.util.Log.d("JenkinsRepo", "authHeader: ${server.authHeader}")
                
                // 先获取 CSRF crumb
                val crumb = fetchCrumbDirect()
                
                // 构建请求
                val formBodyBuilder = FormBody.Builder()
                parameters?.forEach { (key, value) ->
                    formBodyBuilder.add(key, value)
                }
                
                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(formBodyBuilder.build())
                    .header("Authorization", server.authHeader)  // 强制添加认证头
                
                // 添加 CSRF crumb
                if (crumb != null) {
                    android.util.Log.d("JenkinsRepo", "crumb: ${crumb.crumbRequestField}=${crumb.crumb}")
                    requestBuilder.header(crumb.crumbRequestField, crumb.crumb)
                } else {
                    android.util.Log.w("JenkinsRepo", "No crumb available")
                }
                
                val request = requestBuilder.build()
                android.util.Log.d("JenkinsRepo", "Request headers: ${request.headers}")
                
                val response = client.newCall(request).execute()
                val responseCode = response.code
                val responseBody = response.body?.string()
                
                android.util.Log.d("JenkinsRepo", "responseCode: $responseCode")
                android.util.Log.d("JenkinsRepo", "responseBody: ${responseBody?.take(500)}")
                
                when (responseCode) {
                    in 200..299, 201, 302 -> ApiResult.Success(true)
                    401, 403 -> ApiResult.Error("认证失败", responseCode)
                    else -> ApiResult.Error("服务器错误: $responseCode", responseCode)
                }
            } catch (e: Exception) {
                android.util.Log.e("JenkinsRepo", "triggerBuild error", e)
                ApiResult.Error("网络错误: ${e.message}")
            }
        }
    }
    
    /**
     * Fetch CSRF crumb using direct OkHttp call (bypassing Retrofit)
     */
    private fun fetchCrumbDirect(): CrumbResponse? {
        cachedCrumb?.let { return it }
        
        return try {
            val server = currentServer ?: return null
            val client = httpClient ?: return null
            
            val url = "${server.baseUrl}/crumbIssuer/api/json"
            android.util.Log.d("JenkinsRepo", "Fetching crumb from: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", server.authHeader)  // 强制添加认证头
                .header("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            android.util.Log.d("JenkinsRepo", "Crumb response code: ${response.code}")
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val crumb = gson.fromJson(body, CrumbResponse::class.java)
                    cachedCrumb = crumb
                    android.util.Log.d("JenkinsRepo", "Got crumb: ${crumb.crumbRequestField}=${crumb.crumb.take(10)}...")
                    crumb
                } else {
                    null
                }
            } else {
                android.util.Log.w("JenkinsRepo", "Crumb request failed: ${response.code}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("JenkinsRepo", "Crumb request exception", e)
            null
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
    
    private fun buildJobApiUrl(jobUrl: String, suffix: String, query: String? = null): String {
        val baseUri = Uri.parse(jobUrl)
        val builder = baseUri.buildUpon()
        
        var path = baseUri.path ?: ""
        if (!path.endsWith("/")) {
            path += "/"
        }
        path += suffix
        builder.path(path)
        
        if (query != null) {
            builder.encodedQuery(query)
        }
        
        return builder.build().toString()
    }
}
