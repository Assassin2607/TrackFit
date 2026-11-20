package com.example.domain

import com.example.data.remote.GitHubUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateCheckerTest {

    @Test
    fun normalizeRepoInput_acceptsSlugAndFullUrls() {
        assertEquals("harshith/fittrack", GitHubUpdateChecker.normalizeRepoInput("harshith/fittrack"))
        assertEquals("harshith/fittrack", GitHubUpdateChecker.normalizeRepoInput("https://github.com/harshith/fittrack"))
        assertEquals("harshith/fittrack", GitHubUpdateChecker.normalizeRepoInput("https://github.com/harshith/fittrack.git"))
        assertEquals("harshith/fittrack", GitHubUpdateChecker.normalizeRepoInput("github.com/harshith/fittrack/releases"))
        assertNull(GitHubUpdateChecker.normalizeRepoInput(""))
        assertNull(GitHubUpdateChecker.normalizeRepoInput("just-a-name"))
    }

    @Test
    fun isNewerVersion_handlesSemVerComparisons() {
        assertTrue(GitHubUpdateChecker.isNewerVersion("v1.1.0", "1.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("v1.0.1", "1.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("v1.2.0-beta.1", "1.1.5"))

        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.0", "1.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.0.0", "1.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v0.9.9", "1.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("", "1.0"))
    }
}
