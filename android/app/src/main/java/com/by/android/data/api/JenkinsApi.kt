package com.by.android.data.api

import com.by.android.data.model.JenkinsRootResponse
import com.by.android.data.model.JobDetailResponse
import com.by.android.data.model.JobsResponse
import com.by.android.data.model.ViewDetailResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface JenkinsApi {
    
    @GET("api/json")
    suspend fun getServerInfo(): Response<JenkinsRootResponse>
    
    @GET("api/json?tree=jobs[name,url,color,lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url],healthReport[description,score],buildable]")
    suspend fun getAllJobs(): Response<JobsResponse>
    
    @GET("view/{viewName}/api/json")
    suspend fun getViewJobs(@Path("viewName") viewName: String): Response<ViewDetailResponse>
    
    @GET
    suspend fun getJobDetailByUrl(@Url url: String): Response<JobDetailResponse>
    
    @GET("job/{jobName}/{buildNumber}/consoleText")
    suspend fun getConsoleOutput(
        @Path("jobName") jobName: String,
        @Path("buildNumber") buildNumber: Int
    ): Response<ResponseBody>
}
