package com.astute.notes

import android.app.Application
import com.astute.notes.config.AwsConfig
import com.astute.notes.data.LocalNoteRepository
import com.astute.notes.data.S3NoteRepository
import com.astute.notes.data.SyncingNoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AstuteNotesApp : Application() {

    lateinit var repository: SyncingNoteRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        val notesDir = File(filesDir, "notes")
        repository = SyncingNoteRepository(LocalNoteRepository(notesDir))

        AwsConfig.init(this)

        if (AwsConfig.isConfigured) {
            enableS3Sync()
        }
    }

    fun enableS3Sync() {
        val client = AwsConfig.createS3Client() ?: return
        repository.remote = S3NoteRepository(client, AwsConfig.bucketName)
        appScope.launch { repository.sync() }
    }

    fun disableS3Sync() {
        repository.remote = null
    }

    companion object {
        lateinit var instance: AstuteNotesApp
            private set
    }
}
