package com.by.android.ui.builddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.by.android.data.model.BuildDetailResponse
import com.by.android.data.model.BuildParameter
import com.by.android.data.model.BuildResult
import com.by.android.data.model.ParameterDefinition
import com.by.android.data.repository.ApiResult
import com.by.android.data.repository.JenkinsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuildDetailUiState(
    val buildDetail: BuildDetailResponse? = null,
    val logContent: String? = null,
    val isLoading: Boolean = false,
    val isLoadingLog: Boolean = false,
    val isDeleting: Boolean = false,
    val isRebuilding: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteConfirm: Boolean = false,
    val showRebuildDialog: Boolean = false,
    val rebuildParameters: List<ParameterDefinition> = emptyList(),
    val actionMessage: String? = null,
    val shouldNavigateBack: Boolean = false
)

class BuildDetailViewModel(
    private val repository: JenkinsRepository,
    val jobName: String,
    val buildNumber: Int,
    private val buildUrl: String,
    private val jobUrl: String
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BuildDetailUiState())
    val uiState: StateFlow<BuildDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadBuildDetail()
    }
    
    fun loadBuildDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = repository.getBuildDetailByUrl(buildUrl)) {
                is ApiResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            buildDetail = result.data,
                            isLoading = false
                        ) 
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        ) 
                    }
                }
            }
        }
    }
    
    fun loadLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLog = true) }
            
            when (val result = repository.getConsoleOutputByUrl(buildUrl)) {
                is ApiResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            logContent = result.data,
                            isLoadingLog = false
                        ) 
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoadingLog = false,
                            actionMessage = "加载日志失败: ${result.message}"
                        ) 
                    }
                }
            }
        }
    }
    
    fun showDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }
    
    fun hideDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }
    
    fun deleteBuild() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteConfirm = false) }
            
            when (val result = repository.deleteBuildByUrl(buildUrl)) {
                is ApiResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isDeleting = false,
                            actionMessage = "删除成功",
                            shouldNavigateBack = true
                        ) 
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isDeleting = false,
                            actionMessage = "删除失败: ${result.message}"
                        ) 
                    }
                }
            }
        }
    }
    
    fun showRebuildDialog() {
        viewModelScope.launch {
            // Get job parameter definitions
            when (val result = repository.getJobDetailByUrl(jobUrl)) {
                is ApiResult.Success -> {
                    val jobDetail = result.data
                    if (jobDetail.hasParameters) {
                        // Pre-fill with current build's parameters
                        val currentParams = _uiState.value.buildDetail?.buildParameters ?: emptyList()
                        val paramDefs = jobDetail.parameterDefinitions.map { def ->
                            val currentValue = currentParams.find { it.name == def.name }?.value
                            if (currentValue != null) {
                                def.copy(defaultParameterValue = com.by.android.data.model.ParameterValue(
                                    name = def.name,
                                    value = currentValue
                                ))
                            } else {
                                def
                            }
                        }
                        _uiState.update { 
                            it.copy(
                                showRebuildDialog = true,
                                rebuildParameters = paramDefs
                            ) 
                        }
                    } else {
                        // No parameters, rebuild directly
                        rebuild(emptyMap())
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { 
                        it.copy(actionMessage = "获取参数失败: ${result.message}") 
                    }
                }
            }
        }
    }
    
    fun hideRebuildDialog() {
        _uiState.update { it.copy(showRebuildDialog = false) }
    }
    
    fun rebuild(parameters: Map<String, String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRebuilding = true, showRebuildDialog = false) }
            
            when (val result = repository.triggerBuildByUrl(jobUrl, parameters)) {
                is ApiResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isRebuilding = false,
                            actionMessage = "重新构建已触发"
                        ) 
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isRebuilding = false,
                            actionMessage = "重新构建失败: ${result.message}"
                        ) 
                    }
                }
            }
        }
    }
    
    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
