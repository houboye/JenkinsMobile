package com.by.android.ui.jobdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.by.android.data.model.Build
import com.by.android.data.model.JobDetailResponse
import com.by.android.data.model.ParameterDefinition
import com.by.android.data.repository.ApiResult
import com.by.android.data.repository.JenkinsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobDetailUiState(
    val jobDetail: JobDetailResponse? = null,
    val builds: List<Build> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val triggerMessage: String? = null,
    val showParametersDialog: Boolean = false
) {
    /** Check if this job has parameters */
    val hasParameters: Boolean
        get() = jobDetail?.hasParameters == true
    
    /** Get parameter definitions */
    val parameters: List<ParameterDefinition>
        get() = jobDetail?.parameterDefinitions ?: emptyList()
}

class JobDetailViewModel(
    private val repository: JenkinsRepository,
    val jobName: String,
    private val jobUrl: String
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Use URL-based method to support jobs under view paths
            when (val result = repository.getJobDetailByUrl(jobUrl)) {
                is ApiResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            jobDetail = result.data,
                            builds = result.data.builds ?: emptyList(),
                            isLoading = false
                        ) 
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = result.message) 
                    }
                }
            }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            when (val result = repository.getJobDetailByUrl(jobUrl)) {
                is ApiResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            jobDetail = result.data,
                            builds = result.data.builds ?: emptyList(),
                            isRefreshing = false
                        ) 
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }
    
    /** Called when user taps the trigger build button */
    fun onTriggerBuildTapped() {
        if (_uiState.value.hasParameters) {
            // Show parameters dialog
            _uiState.update { it.copy(showParametersDialog = true) }
        } else {
            // No parameters, trigger directly
            triggerBuild(null)
        }
    }
    
    /** Hide the parameters dialog */
    fun hideParametersDialog() {
        _uiState.update { it.copy(showParametersDialog = false) }
    }
    
    /** Trigger build with optional parameters */
    fun triggerBuild(parameters: Map<String, String>?) {
        viewModelScope.launch {
            // Hide dialog first
            _uiState.update { it.copy(showParametersDialog = false) }
            
            when (val result = repository.triggerBuildByUrl(jobUrl, parameters)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(triggerMessage = "已触发构建") }
                    kotlinx.coroutines.delay(1000)
                    refresh()
                }
                is ApiResult.Error -> {
                    _uiState.update { 
                        it.copy(triggerMessage = "触发失败: ${result.message}") 
                    }
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun clearTriggerMessage() {
        _uiState.update { it.copy(triggerMessage = null) }
    }
}

