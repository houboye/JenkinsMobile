package com.by.android.ui.buildlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.by.android.data.repository.ApiResult
import com.by.android.data.repository.JenkinsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuildLogUiState(
    val logContent: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class BuildLogViewModel(
    private val repository: JenkinsRepository,
    val jobName: String,
    val buildNumber: Int,
    private val buildUrl: String
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BuildLogUiState())
    val uiState: StateFlow<BuildLogUiState> = _uiState.asStateFlow()
    
    fun loadLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = repository.getConsoleOutputByUrl(buildUrl)) {
                is ApiResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            logContent = result.data,
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
}
