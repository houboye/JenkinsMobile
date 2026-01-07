package com.by.android.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.by.android.data.model.JenkinsView
import com.by.android.data.model.Job
import com.by.android.data.model.ParameterDefinition
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
    val triggerMessage: String? = null,
    // 参数对话框状态
    val showParametersDialog: Boolean = false,
    val pendingJob: Job? = null,
    val parameters: List<ParameterDefinition> = emptyList()
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
    
    /** 点击构建按钮时调用 - 先检查是否有参数 */
    fun triggerBuild(job: Job) {
        viewModelScope.launch {
            // 先获取 job 详情，检查是否有参数
            when (val result = repository.getJobDetailByUrl(job.url)) {
                is ApiResult.Success -> {
                    val jobDetail = result.data
                    if (jobDetail.hasParameters) {
                        // 有参数，显示参数对话框
                        _uiState.update {
                            it.copy(
                                showParametersDialog = true,
                                pendingJob = job,
                                parameters = jobDetail.parameterDefinitions
                            )
                        }
                    } else {
                        // 无参数，直接触发构建
                        executeTriggerBuild(job.url, null)
                    }
                }
                is ApiResult.Error -> {
                    // 获取详情失败，尝试直接构建
                    executeTriggerBuild(job.url, null)
                }
            }
        }
    }
    
    /** 带参数触发构建 */
    fun triggerBuildWithParameters(parameters: Map<String, String>) {
        val job = _uiState.value.pendingJob ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showParametersDialog = false) }
            executeTriggerBuild(job.url, parameters)
        }
    }
    
    /** 执行构建 */
    private suspend fun executeTriggerBuild(jobUrl: String, parameters: Map<String, String>?) {
        when (val result = repository.triggerBuildByUrl(jobUrl, parameters)) {
            is ApiResult.Success -> {
                _uiState.update { 
                    it.copy(
                        triggerMessage = "已触发构建",
                        pendingJob = null
                    ) 
                }
                kotlinx.coroutines.delay(1000)
                refresh()
            }
            is ApiResult.Error -> {
                _uiState.update { 
                    it.copy(
                        triggerMessage = "触发失败: ${result.message}",
                        pendingJob = null
                    ) 
                }
            }
        }
    }
    
    /** 隐藏参数对话框 */
    fun hideParametersDialog() {
        _uiState.update { 
            it.copy(
                showParametersDialog = false,
                pendingJob = null,
                parameters = emptyList()
            ) 
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun clearTriggerMessage() {
        _uiState.update { it.copy(triggerMessage = null) }
    }
}
