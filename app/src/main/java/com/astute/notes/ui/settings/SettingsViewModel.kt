package com.astute.notes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.notes.AstuteNotesApp
import com.astute.notes.config.AwsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val region: String = "us-east-1",
    val bucketName: String = "",
    val backupStatus: String? = null,
    val isBackingUp: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = SettingsUiState(
            accessKeyId = AwsConfig.accessKeyId,
            secretAccessKey = AwsConfig.secretAccessKey,
            region = AwsConfig.region,
            bucketName = AwsConfig.bucketName
        )
    }

    fun onAccessKeyIdChanged(value: String) {
        _uiState.value = _uiState.value.copy(accessKeyId = value)
    }

    fun onSecretAccessKeyChanged(value: String) {
        _uiState.value = _uiState.value.copy(secretAccessKey = value)
    }

    fun onRegionChanged(value: String) {
        _uiState.value = _uiState.value.copy(region = value)
    }

    fun onBucketNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(bucketName = value)
    }

    fun saveCredentials() {
        val state = _uiState.value
        AwsConfig.accessKeyId = state.accessKeyId.trim()
        AwsConfig.secretAccessKey = state.secretAccessKey.trim()
        AwsConfig.region = state.region.trim().ifBlank { "us-east-1" }
        AwsConfig.bucketName = state.bucketName.trim()
        _uiState.value = _uiState.value.copy(backupStatus = "Credentials saved")
    }

    fun clearCredentials() {
        AwsConfig.clearCredentials()
        _uiState.value = SettingsUiState(backupStatus = "Credentials cleared")
    }

    fun backupNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUp = true, backupStatus = null)
            try {
                val app = AstuteNotesApp.instance
                val s3Repo = app.createS3Repository()
                if (s3Repo == null) {
                    _uiState.value = _uiState.value.copy(
                        isBackingUp = false,
                        backupStatus = "AWS credentials not configured"
                    )
                    return@launch
                }
                val notes = app.repository.listNotes()
                s3Repo.backupNotes(notes)
                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    backupStatus = "Backed up ${notes.size} note(s) to S3"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    backupStatus = "Backup failed: ${e.message}"
                )
            }
        }
    }
}
