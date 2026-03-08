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
    val syncStatus: String? = null,
    val isSyncing: Boolean = false
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

        if (AwsConfig.isConfigured) {
            AstuteNotesApp.instance.enableS3Sync()
            _uiState.value = _uiState.value.copy(syncStatus = "Credentials saved — syncing enabled")
        } else {
            _uiState.value = _uiState.value.copy(syncStatus = "Credentials saved")
        }
    }

    fun clearCredentials() {
        AwsConfig.clearCredentials()
        AstuteNotesApp.instance.disableS3Sync()
        _uiState.value = SettingsUiState(syncStatus = "Credentials cleared — syncing disabled")
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatus = null)
            try {
                val app = AstuteNotesApp.instance
                if (app.repository.remote == null) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncStatus = "AWS credentials not configured"
                    )
                    return@launch
                }
                app.repository.sync()
                val count = app.repository.listNotes().size
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncStatus = "Synced $count note(s) with S3"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncStatus = "Sync failed: ${e.message}"
                )
            }
        }
    }
}
