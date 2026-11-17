package com.aether.assistant.core.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small, dependency-free helpers used across the app. */
object AppTime {
    fun now(): Long = System.currentTimeMillis()

    fun readable(ts: Long): String =
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(ts))
}

inline fun <T> Result<T>.getOrElseSafe(fallback: T): T = getOrElse { fallback }

suspend inline fun <T> runOnIo(crossinline block: () -> T): T =
    withContext(Dispatchers.IO) { block() }

fun String.truncate(max: Int): String =
    if (length <= max) this else take(max - 1) + "…"

/** Very small keyword scorer used by the offline intent router. */
fun String.containsAny(vararg words: String): Boolean {
    val lower = lowercase(Locale.getDefault())
    return words.any { lower.contains(it.lowercase(Locale.getDefault())) }
}
