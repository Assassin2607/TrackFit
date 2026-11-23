package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.ApkDownloadState
import com.example.data.remote.AppUpdateInfo
import com.example.data.remote.InAppApkUpdater
import com.example.domain.calc.FitnessCalculations
import com.example.ui.theme.FitTrackThemeExt
import com.example.ui.theme.MonoMetricSmall
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Computes Monday (first day of the current week) as a yyyy-MM-dd date key.
 */
fun currentWeekMondayDateKey(): String {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val daysSinceMonday = if (dow == Calendar.SUNDAY) 6 else (dow - Calendar.MONDAY)
    cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

/**
 * Non-destructive reset dialog allowing users to reset a dashboard metric
 * (7-Day Volume or Weight & Trend) from Right Now, Monday (First of Week), or any recent day
 * while keeping 100% of past logs preserved in Workout History and Progress.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricBaselineResetDialog(
    title: String,
    preservedNote: String,
    activeResetDateKey: String,
    activeResetEpochMs: Long,
    onApplyReset: (dateKey: String, epochMs: Long) -> Unit,
    onClearReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = FitTrackThemeExt.colors
    val creamText = if (ext.isLightTheme) MaterialTheme.colorScheme.onSurface else Color(0xFFFFF5E1)
    val beigeAccent = if (ext.isLightTheme) MaterialTheme.colorScheme.primary else Color(0xFFF3DEC0)
    val beigeMuted = if (ext.isLightTheme) ext.mutedText else Color(0xFFE2D3BE)
    val dialogContainer = if (ext.isLightTheme) MaterialTheme.colorScheme.surface else Color(0xFF1A1E36)
    val optionCardBg = if (ext.isLightTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else Color(0xFF242A4A)
    val optionCardBorder = if (ext.isLightTheme) MaterialTheme.colorScheme.outline.copy(alpha = 0.6f) else beigeAccent.copy(alpha = 0.28f)

    val todayKey = remember { FitnessCalculations.todayDateKey() }
    val mondayKey = remember { currentWeekMondayDateKey() }
    val recentDays = remember {
        val parseFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("EEE d", Locale.US)
        (0..6).map { daysAgo ->
            val dk = FitnessCalculations.dateKeyDaysAgo(daysAgo)
            val parsed: Date? = runCatching { parseFmt.parse(dk) }.getOrNull()
            val label = when (daysAgo) {
                0 -> "Today"
                1 -> "Yesterday"
                else -> parsed?.let { labelFmt.format(it) } ?: dk
            }
            dk to label
        }
    }
    var selectedCustomDate by remember(activeResetDateKey) {
        mutableStateOf(activeResetDateKey.ifBlank { todayKey })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogContainer,
        titleContentColor = creamText,
        textContentColor = beigeMuted,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    tint = beigeAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = creamText
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = preservedNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = beigeMuted
                )

                if (activeResetDateKey.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = beigeAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, beigeAccent.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onClearReset()
                                onDismiss()
                            }
                            .testTag("reset_restore_all")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = beigeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Restore Full History",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = creamText
                                )
                                Text(
                                    text = "Clear reset and include all logged days again",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = beigeMuted
                                )
                            }
                        }
                    }
                }

                // Quick Preset 1: Start Fresh Right Now (zeroes out earlier test logs from today too)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = optionCardBg,
                    border = BorderStroke(1.dp, optionCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onApplyReset(todayKey, System.currentTimeMillis())
                            onDismiss()
                        }
                        .testTag("reset_start_fresh_now")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Today,
                            contentDescription = null,
                            tint = beigeAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Fresh Right Now",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = creamText
                            )
                            Text(
                                text = "Resets counter to 0 from this moment onward ($todayKey)",
                                style = MaterialTheme.typography.labelSmall,
                                color = beigeMuted
                            )
                        }
                    }
                }

                // Quick Preset 2: Start from First of This Week (Monday)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = optionCardBg,
                    border = BorderStroke(1.dp, optionCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onApplyReset(mondayKey, 0L)
                            onDismiss()
                        }
                        .testTag("reset_start_monday")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = beigeAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start from First of Week (Mon)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = creamText
                            )
                            Text(
                                text = "Count logs from Monday ($mondayKey) onward",
                                style = MaterialTheme.typography.labelSmall,
                                color = beigeMuted
                            )
                        }
                    }
                }

                // Custom Day Picker from last 7 days
                Text(
                    text = "Or pick a specific start day:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = creamText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recentDays.forEach { (dk, label) ->
                        FilterChip(
                            selected = selectedCustomDate == dk,
                            onClick = { selectedCustomDate = dk },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedCustomDate == dk) creamText else beigeMuted
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyReset(selectedCustomDate, 0L)
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = beigeAccent,
                    contentColor = Color(0xFF141729)
                ),
                modifier = Modifier.testTag("confirm_custom_reset_button")
            ) {
                Text("Start from $selectedCustomDate", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (activeResetDateKey.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onClearReset()
                            onDismiss()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = beigeAccent
                        )
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore", fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = beigeMuted
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Shared in-app APK updater control bar (used in both Home Banner and Profile -> App Updates).
 * Downloads the release APK inside FitTrack with a live progress bar and triggers Android's
 * Package Installer directly without redirecting to Chrome.
 */
@Composable
fun InAppUpdateActionPanel(
    info: AppUpdateInfo,
    downloadState: ApkDownloadState,
    onStartDownload: (AppUpdateInfo) -> Unit,
    onCancelDownload: () -> Unit,
    onInstallReadyApk: () -> Unit,
    onDismissBanner: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ext = FitTrackThemeExt.colors
    Column(modifier = modifier.fillMaxWidth()) {
        when (downloadState) {
            is ApkDownloadState.Downloading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloading ${downloadState.tagName}…",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (downloadState.totalBytes > 0L) {
                            "${downloadState.progressPercent}% (${InAppApkUpdater.formatBytesMb(downloadState.downloadedBytes)} / ${InAppApkUpdater.formatBytesMb(downloadState.totalBytes)})"
                        } else {
                            InAppApkUpdater.formatBytesMb(downloadState.downloadedBytes)
                        },
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (downloadState.totalBytes > 0L) {
                    LinearProgressIndicator(
                        progress = { (downloadState.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancelDownload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Download")
                }
            }

            is ApkDownloadState.NeedsInstallPermission -> {
                Text(
                    text = "Update ${downloadState.tagName} downloaded! Allow \"Install unknown apps\" for FitTrack once so updates install right inside the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.warning
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onInstallReadyApk,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grant_install_permission_button")
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Allow & Install")
                    }
                    if (onDismissBanner != null) {
                        TextButton(onClick = onDismissBanner) {
                            Text("Later")
                        }
                    }
                }
            }

            is ApkDownloadState.ReadyToInstall -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onInstallReadyApk,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("install_downloaded_apk_button")
                    ) {
                        Icon(Icons.Rounded.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Install ${downloadState.tagName}")
                    }
                    if (onDismissBanner != null) {
                        TextButton(onClick = onDismissBanner) {
                            Text("Later")
                        }
                    }
                }
            }

            is ApkDownloadState.Error -> {
                Text(
                    text = downloadState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStartDownload(info) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_update_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Update")
                    }
                    if (onDismissBanner != null) {
                        TextButton(onClick = onDismissBanner) {
                            Text("Later")
                        }
                    }
                }
            }

            ApkDownloadState.Idle -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStartDownload(info) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_update_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Now (${info.tagName})")
                    }
                    if (onDismissBanner != null) {
                        TextButton(onClick = onDismissBanner) {
                            Text("Later")
                        }
                    }
                }
            }
        }
    }
}
