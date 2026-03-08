package com.astutenotes

import android.app.Application
import com.astutenotes.config.AwsConfig
import com.astutenotes.data.LocalNoteRepository
import com.astutenotes.data.NoteRepository
import com.astutenotes.data.S3NoteRepository
import java.io.File

class AstuteNotesApp : Application() {

    lateinit var repository: NoteRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val notesDir = File(filesDir, "notes")
        repository = LocalNoteRepository(notesDir)

        AwsConfig.init(this)
    }

    fun createS3Repository(): S3NoteRepository? {
        val client = AwsConfig.createS3Client() ?: return null
        return S3NoteRepository(client, AwsConfig.bucketName)
    }

    companion object {
        lateinit var instance: AstuteNotesApp
            private set
    }
}
