package com.astute.notes.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSerializationTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun `serialize and deserialize note round-trips correctly`() {
        val note = Note(
            id = "abc-123",
            title = "Test Title",
            body = "Test Body",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val jsonString = json.encodeToString(note)
        val deserialized = json.decodeFromString<Note>(jsonString)

        assertEquals(note, deserialized)
    }

    @Test
    fun `serialized json contains expected fields`() {
        val note = Note(
            id = "id-1",
            title = "My Note",
            body = "Content here",
            createdAt = 100L,
            updatedAt = 200L
        )

        val jsonString = json.encodeToString(note)

        assertTrue(jsonString.contains("\"id\""))
        assertTrue(jsonString.contains("\"title\""))
        assertTrue(jsonString.contains("\"body\""))
        assertTrue(jsonString.contains("\"createdAt\""))
        assertTrue(jsonString.contains("\"updatedAt\""))
        assertTrue(jsonString.contains("id-1"))
        assertTrue(jsonString.contains("My Note"))
        assertTrue(jsonString.contains("Content here"))
    }

    @Test
    fun `deserialization ignores unknown keys`() {
        val jsonString = """
            {
                "id": "test-id",
                "title": "Title",
                "body": "Body",
                "createdAt": 100,
                "updatedAt": 200,
                "unknownField": "should be ignored"
            }
        """.trimIndent()

        val note = json.decodeFromString<Note>(jsonString)
        assertEquals("test-id", note.id)
        assertEquals("Title", note.title)
    }

    @Test
    fun `note with empty strings serializes correctly`() {
        val note = Note(id = "1", title = "", body = "", createdAt = 0, updatedAt = 0)
        val jsonString = json.encodeToString(note)
        val deserialized = json.decodeFromString<Note>(jsonString)
        assertEquals(note, deserialized)
    }

    @Test
    fun `note with special characters serializes correctly`() {
        val note = Note(
            id = "1",
            title = "Title with \"quotes\" and \nnewlines",
            body = "Body with unicode: \u00e9\u00e8\u00ea and emoji-like chars",
            createdAt = 0,
            updatedAt = 0
        )
        val jsonString = json.encodeToString(note)
        val deserialized = json.decodeFromString<Note>(jsonString)
        assertEquals(note, deserialized)
    }

    @Test
    fun `note copy preserves all fields`() {
        val original = Note("id", "title", "body", 100L, 200L)
        val copy = original.copy(title = "new title")

        assertEquals("id", copy.id)
        assertEquals("new title", copy.title)
        assertEquals("body", copy.body)
        assertEquals(100L, copy.createdAt)
        assertEquals(200L, copy.updatedAt)
    }
}
