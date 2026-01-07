package com.by.android

import android.app.Application
import com.by.android.data.repository.JenkinsRepository

class JenkinsApp : Application() {
    
    lateinit var repository: JenkinsRepository
        private set
    
    override fun onCreate() {
        super.onCreate()
        repository = JenkinsRepository(this)
    }
}

