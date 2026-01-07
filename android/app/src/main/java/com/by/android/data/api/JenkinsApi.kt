package com.by.android.data.api

import com.by.android.data.model.Build
import com.by.android.data.model.CrumbResponse
import com.by.android.data.model.JenkinsRootResponse
import com.by.android.data.model.JobDetailResponse
import com.by.android.data.model.JobsResponse
import com.by.android.data.model.ViewDetailResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

interface JenkinsApi {
    
    @GET("api/json")
    suspend fun getServerInfo(): Response<JenkinsRootResponse>
    
    @GET("crumbIssuer/api/json")
    suspend fun getCrumb(): Response<CrumbResponse>
    
    @GET("api/json?tree=jobs[name,url,color,lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url],healthReport[description,score],buildable]")
    suspend fun getAllJobs(): Response<JobsResponse>
    
    @GET("view/{viewName}/api/json")
    suspend fun getViewJobs(@Path("viewName") viewName: String): Response<ViewDetailResponse>
    
    @GET("job/{jobName}/api/json?tree=name,url,color,description,buildable,builds[number,url,result,timestamp,duration,displayName,building,description],lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url]")
    suspend fun getJobDetail(@Path("jobName") jobName: String): Response<JobDetailResponse>
    
    // Use full URL for jobs that might be under a view path
    @GET
    suspend fun getJobDetailByUrl(@Url url: String): Response<JobDetailResponse>
    
    @GET("job/{jobName}/{buildNumber}/api/json")
    suspend fun getBuild(
        @Path("jobName") jobName: String,
        @Path("buildNumber") buildNumber: Int
    ): Response<Build>
    
    // Use buildWithParameters for both parameterized and non-parameterized jobs
    @FormUrlEncoded
    @POST("job/{jobName}/buildWithParameters")
    suspend fun triggerBuild(
        @Path("jobName") jobName: String,
        @Header("Jenkins-Crumb") crumb: String?,
        @FieldMap parameters: Map<String, String>
    ): Response<ResponseBody>
    
    // Non-parameterized version
    @POST("job/{jobName}/buildWithParameters")
    suspend fun triggerBuildSimple(
        @Path("jobName") jobName: String,
        @Header("Jenkins-Crumb") crumb: String?
    ): Response<ResponseBody>
    
    // Use full URL for triggering builds on jobs under a view path
    @FormUrlEncoded
    @POST
    suspend fun triggerBuildByUrl(
        @Url url: String,
        @Header("Jenkins-Crumb") crumb: String?,
        @FieldMap parameters: Map<String, String>
    ): Response<ResponseBody>
    
    // Non-parameterized version with full URL
    @POST
    suspend fun triggerBuildByUrlSimple(
        @Url url: String,
        @Header("Jenkins-Crumb") crumb: String?
    ): Response<ResponseBody>
    
    @GET("job/{jobName}/{buildNumber}/consoleText")
    suspend fun getConsoleOutput(
        @Path("jobName") jobName: String,
        @Path("buildNumber") buildNumber: Int
    ): Response<ResponseBody>
}

