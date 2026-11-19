package com.example.engine

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed interface JsonValidationResult {
    data class Valid(val formatted: String, val minified: String) : JsonValidationResult
    data class Invalid(val message: String) : JsonValidationResult
}

object TextDocumentEngine {

    fun readText(file: File): String =
        runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("")

    fun writeText(content: String, file: File) {
        file.writeText(content, Charsets.UTF_8)
    }

    fun countWords(content: String): Int =
        content.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    fun validateAndFormatJson(raw: String): JsonValidationResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return JsonValidationResult.Invalid("Empty JSON content")
        return try {
            if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                JsonValidationResult.Valid(
                    formatted = obj.toString(2),
                    minified = obj.toString(),
                )
            } else if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                JsonValidationResult.Valid(
                    formatted = arr.toString(2),
                    minified = arr.toString(),
                )
            } else {
                JsonValidationResult.Invalid("JSON must start with '{' or '['")
            }
        } catch (e: Exception) {
            JsonValidationResult.Invalid(e.message ?: "Invalid JSON syntax")
        }
    }

    fun exportTextToPdf(title: String, content: String, outputFile: File) {
        val doc = WordDocumentEngine.fromMarkdown(content)
        WordDocumentEngine.exportToPdf(title, doc, outputFile)
    }
}
