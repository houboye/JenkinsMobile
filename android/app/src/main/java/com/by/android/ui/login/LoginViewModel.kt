package com.by.android.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.by.android.data.model.Server
import com.by.android.data.repository.ApiResult
import com.by.android.data.repository.JenkinsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val apiToken: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

class LoginViewModel(
    private val repository: JenkinsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    init {
        loadSavedServer()
    }
    
    private fun loadSavedServer() {
        viewModelScope.launch {
            val server = repository.serverFlow.first()
            _uiState.update { currentState ->
                currentState.copy(
                    serverUrl = server.url,
                    username = server.username,
                    apiToken = server.apiToken
                )
            }
        }
    }
    
    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url, errorMessage = null) }
    }
    
    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username, errorMessage = null) }
    }
    
    fun updateApiToken(token: String) {
        _uiState.update { it.copy(apiToken = token, errorMessage = null) }
    }
    
    fun isFormValid(): Boolean {
        return _uiState.value.serverUrl.isNotBlank() &&
                _uiState.value.username.isNotBlank() &&
                _uiState.value.apiToken.isNotBlank()
    }
    
    fun login() {
        if (!isFormValid()) {
            _uiState.update { it.copy(errorMessage = "请填写所有字段") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val server = Server(
                url = _uiState.value.serverUrl.trim(),
                username = _uiState.value.username.trim(),
                apiToken = _uiState.value.apiToken.trim()
            )
            
            when (val result = repository.login(server)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                }
                is ApiResult.Error -> {
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = result.message) 
                    }
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

