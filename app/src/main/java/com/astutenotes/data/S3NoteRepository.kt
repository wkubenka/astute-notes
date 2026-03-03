package com.astutenotes.data

import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import com.astutenotes.config.AwsConfig
import com.astutenotes.model.Note
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class S3NoteRepository {

    private val s3 = AwsConfig.s3Client
    private val bucket = AwsConfig.bucketName
    private val prefix = AwsConfig.NOTES_PREFIX
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun listNotes(): List<Note> {
        val response = s3.listObjectsV2(ListObjectsV2Request {
            this.bucket = this@S3NoteRepository.bucket
            this.prefix = this@S3NoteRepository.prefix
        })

        val notes = mutableListOf<Note>()
        response.contents?.forEach { obj ->
            val key = obj.key ?: return@forEach
            if (key.endsWith(".json")) {
                try {
                    val note = getNote(key)
                    if (note != null) notes.add(note)
                } catch (_: Exception) {
                    // Skip malformed notes
                }
            }
        }
        return notes.sortedByDescending { it.updatedAt }
    }

    suspend fun getNoteById(id: String): Note? {
        return getNote("${prefix}${id}.json")
    }

    suspend fun createNote(title: String, body: String): Note {
        val now = System.currentTimeMillis()
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            createdAt = now,
            updatedAt = now
        )
        putNote(note)
        return note
    }

    suspend fun updateNote(note: Note): Note {
        val updated = note.copy(updatedAt = System.currentTimeMillis())
        putNote(updated)
        return updated
    }

    suspend fun deleteNote(id: String) {
        s3.deleteObject(DeleteObjectRequest {
            this.bucket = this@S3NoteRepository.bucket
            this.key = "${prefix}${id}.json"
        })
    }

    private suspend fun getNote(key: String): Note? {
        return try {
            s3.getObject(GetObjectRequest {
                this.bucket = this@S3NoteRepository.bucket
                this.key = key
            }) { response ->
                val content = response.body?.decodeToString() ?: return@getObject null
                json.decodeFromString<Note>(content)
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun putNote(note: Note) {
        val jsonString = json.encodeToString(note)
        s3.putObject(PutObjectRequest {
            this.bucket = this@S3NoteRepository.bucket
            this.key = "${prefix}${note.id}.json"
            this.contentType = "application/json"
            this.body = ByteStream.fromString(jsonString)
        })
    }
}
