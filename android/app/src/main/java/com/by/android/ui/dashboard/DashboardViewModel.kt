package com.by.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.by.android.data.model.JenkinsView
import com.by.android.data.model.Job
import com.by.android.data.repository.ApiResult
import com.by.android.data.repository.JenkinsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val views: List<JenkinsView> = emptyList(),
    val jobs: List<Job> = emptyList(),
    val selectedViewIndex: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val triggerMessage: String? = null
)

class DashboardViewModel(
    private val repository: JenkinsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = repository.getViews()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(views = result.data) }
                    loadJobsForSelectedView()
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
            loadJobsForSelectedView()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
    
    fun selectView(index: Int) {
        if (index == _uiState.value.selectedViewIndex) return
        
        _uiState.update { it.copy(selectedViewIndex = index) }
        viewModelScope.launch {
            loadJobsForSelectedView()
        }
    }
    
    private suspend fun loadJobsForSelectedView() {
        val views = _uiState.value.views
        val selectedIndex = _uiState.value.selectedViewIndex
        
        val result = if (views.isEmpty() || selectedIndex >= views.size) {
            repository.getAllJobs()
        } else {
            val selectedView = views[selectedIndex]
            if (selectedView.name.equals("all", ignoreCase = true)) {
                repository.getAllJobs()
            } else {
                repository.getViewJobs(selectedView.name)
            }
        }
        
        when (result) {
            is ApiResult.Success -> {
                _uiState.update { 
                    it.copy(jobs = result.data, isLoading = false) 
                }
            }
            is ApiResult.Error -> {
                _uiState.update { 
                    it.copy(isLoading = false, errorMessage = result.message) 
                }
            }
        }
    }
    
    fun triggerBuild(job: Job) {
        viewModelScope.launch {
            when (val result = repository.triggerBuild(job.name)) {
                is ApiResult.Success -> {
                    _uiState.update { 
                        it.copy(triggerMessage = "已触发构建: ${job.name}") 
                    }
                    // Refresh after a short delay
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

