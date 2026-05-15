package com.example.messenger.data

internal fun isUnauthenticatedError(message: String?): Boolean {
    val normalized = message?.trim()?.lowercase() ?: return false
    return "unauthenticated" in normalized || "unauthorized" in normalized
}
