package com.by.android.data.api

import com.by.android.data.model.Build
import com.by.android.data.model.JenkinsRootResponse
import com.by.android.data.model.JobDetailResponse
import com.by.android.data.model.JobsResponse
import com.by.android.data.model.ViewDetailResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface JenkinsApi {
    
    @GET("api/json")
    suspend fun getServerInfo(): Response<JenkinsRootResponse>
    
    @GET("api/json?tree=jobs[name,url,color,lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url],healthReport[description,score],buildable]")
    suspend fun getAllJobs(): Response<JobsResponse>
    
    @GET("view/{viewName}/api/json")
    suspend fun getViewJobs(@Path("viewName") viewName: String): Response<ViewDetailResponse>
    
    @GET("job/{jobName}/api/json?tree=name,url,color,description,buildable,builds[number,url,result,timestamp,duration,displayName,building,description],lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url]")
    suspend fun getJobDetail(@Path("jobName") jobName: String): Response<JobDetailResponse>
    
    @GET("job/{jobName}/{buildNumber}/api/json")
    suspend fun getBuild(
        @Path("jobName") jobName: String,
        @Path("buildNumber") buildNumber: Int
    ): Response<Build>
    
    @POST("job/{jobName}/build")
    suspend fun triggerBuild(@Path("jobName") jobName: String): Response<ResponseBody>
    
    @GET("job/{jobName}/{buildNumber}/consoleText")
    suspend fun getConsoleOutput(
        @Path("jobName") jobName: String,
        @Path("buildNumber") buildNumber: Int
    ): Response<ResponseBody>
}

