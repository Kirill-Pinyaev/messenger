package com.example.messenger.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorsTest {
    @Test
    fun `detects unauthenticated grpc status`() {
        assertTrue(isUnauthenticatedError("UNAUTHENTICATED: unauthorized"))
    }

    @Test
    fun `detects unauthorized text`() {
        assertTrue(isUnauthenticatedError("unauthorized"))
    }

    @Test
    fun `ignores unrelated errors`() {
        assertFalse(isUnauthenticatedError("NOT_FOUND: conversation not found"))
        assertFalse(isUnauthenticatedError(null))
    }
}
