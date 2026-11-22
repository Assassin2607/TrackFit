package com.example.domain

import com.example.data.remote.GitHubUpdateChecker
import com.example.data.remote.InAppApkUpdater
import com.example.notifications.FitTrackNotificationManager
import java.util.Calendar
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

    @Test
    fun formatBytesMb_formatsApkDownloadSizes() {
        assertEquals("10.0 MB", InAppApkUpdater.formatBytesMb(10L * 1024L * 1024L))
        assertEquals("0 MB", InAppApkUpdater.formatBytesMb(0L))
        assertEquals("2.5 MB", InAppApkUpdater.formatBytesMb((2.5 * 1024.0 * 1024.0).toLong()))
    }

    @Test
    fun formatTime12Hour_andDayLabels_formatPersonalReminderTimes() {
        assertEquals("12:05 AM", FitTrackNotificationManager.formatTime12Hour(0, 5))
        assertEquals("8:00 AM", FitTrackNotificationManager.formatTime12Hour(8, 0))
        assertEquals("12:00 PM", FitTrackNotificationManager.formatTime12Hour(12, 0))
        assertEquals("1:00 PM", FitTrackNotificationManager.formatTime12Hour(13, 0))
        assertEquals("6:30 PM", FitTrackNotificationManager.formatTime12Hour(18, 30))

        assertEquals("Sundays", FitTrackNotificationManager.dayOfWeekLabel(Calendar.SUNDAY))
        assertEquals("Mon", FitTrackNotificationManager.dayOfWeekShort(Calendar.MONDAY))
    }

    @Test
    fun computeNextTriggerMillis_alwaysSchedulesInFuture() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 19, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerDaily = FitTrackNotificationManager.computeNextTriggerMillis(
            hourOfDay = 18,
            minute = 30,
            weeklyDayOfWeek = null,
            nowMillis = baseCal.timeInMillis
        )
        assertTrue("Daily trigger must advance to next day when time has passed", triggerDaily > baseCal.timeInMillis)
        val dailyCal = Calendar.getInstance().apply { timeInMillis = triggerDaily }
        assertEquals(16, dailyCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, dailyCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, dailyCal.get(Calendar.MINUTE))

        val triggerWeekly = FitTrackNotificationManager.computeNextTriggerMillis(
            hourOfDay = 9,
            minute = 15,
            weeklyDayOfWeek = Calendar.SUNDAY,
            nowMillis = baseCal.timeInMillis
        )
        assertTrue("Weekly trigger must be in the future", triggerWeekly > baseCal.timeInMillis)
        val cal = Calendar.getInstance().apply { timeInMillis = triggerWeekly }
        assertEquals(Calendar.SUNDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(Calendar.MINUTE))
    }
}
