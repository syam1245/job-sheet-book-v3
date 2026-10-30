package com.example.jobsheetbook.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jobsheetbook.data.sync.SyncResult
import com.example.jobsheetbook.ui.theme.*
import com.example.jobsheetbook.ui.viewmodel.JobViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: JobViewModel = hiltViewModel(),
    onOpenTrash: () -> Unit = {}
) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val dashboardState by viewModel.dashboardUiState.collectAsState()
    val totalJobs = dashboardState.activeJobsCount
    val formattedSyncTime = remember(lastSyncTime) {
        lastSyncTime?.let {
            SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault()).format(Date(it))
        } ?: "Not synced yet this session"
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Top Header ──
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(DashboardBlue)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Admin info solution • Cloud & Storage",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Card(
                onClick = onOpenTrash,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Trash Bin", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Find and restore deleted jobs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Appearance / Theme Card ──
            val themeMode by viewModel.themeMode.collectAsState()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Theme Mode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.jobsheetbook.data.local.ThemeMode.entries.forEach { mode ->
                                val isSelected = themeMode == mode
                                Surface(
                                    modifier = Modifier.clickable { viewModel.setThemeMode(mode) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) DashboardBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) DashboardBlue else MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        color = if (isSelected) DashboardBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Cloud Sync Card ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DashboardBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudSync,
                                    contentDescription = null,
                                    tint = DashboardBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Firestore Cloud Sync",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Two-way offline-first sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Online/Offline status pill
                        Surface(
                            color = StatusReady.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(StatusReady)
                                )
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = StatusReady
                                )
                            }
                        }
                    }

                    Text(
                        text = "Synchronize local offline data with Firebase Firestore cloud. Merges changes using the latest updated timestamps in both directions so no data is lost.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    // Sync details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last Synced",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedSyncTime,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // ── Manual Sync Button ──
                    Button(
                        onClick = { viewModel.triggerManualSync() },
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DashboardBlue,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = DashboardBlue.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Synchronizing Database...",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Manual Sync Database Now",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // ── Sync Result Alert ──
                    AnimatedVisibility(visible = syncResult != null) {
                        syncResult?.let { result ->
                            SyncResultBanner(result = result)
                        }
                    }
                }
            }
            // ── Data & Backups Card ──
            var showDatabaseExportDialog by remember { mutableStateOf(false) }
            var showCustomerExportDialog by remember { mutableStateOf(false) }
            var showRangeReportDialog by remember { mutableStateOf(false) }
            val context = androidx.compose.ui.platform.LocalContext.current

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Data & Backups",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = { showDatabaseExportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Download Database Backup")
                    }

                    OutlinedButton(
                        onClick = { showCustomerExportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Download Customer List for Follow-Up")
                    }

                    OutlinedButton(
                        onClick = { showRangeReportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Report by Date Range")
                    }
                }
            }

            if (showDatabaseExportDialog) {
                ExportDialog(
                    title = "Export Database",
                    scopeOptions = listOf("Active Only", "Full History"),
                    formatOptions = listOf("CSV", "JSON"),
                    onExport = { scope, format ->
                        viewModel.exportDatabase(scope, format) { file ->
                            shareExportedFile(context, file, format)
                        }
                        showDatabaseExportDialog = false
                    },
                    onDismiss = { showDatabaseExportDialog = false }
                )
            }

            if (showCustomerExportDialog) {
                ExportDialog(
                    title = "Export Customer List",
                    scopeOptions = listOf("Active Customers", "All Customers"),
                    formatOptions = listOf("CSV", "JSON"),
                    onExport = { scope, format ->
                        viewModel.exportCustomerList(scope, format) { file ->
                            shareExportedFile(context, file, format)
                        }
                        showCustomerExportDialog = false
                    },
                    onDismiss = { showCustomerExportDialog = false }
                )
            }

            if (showRangeReportDialog) {
                DateRangeReportDialog(
                    onGenerate = { startDate, endDate ->
                        viewModel.generateAndCopyRangeReport(context, startDate, endDate) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Delivery report copied to clipboard!")
                            }
                        }
                        showRangeReportDialog = false
                    },
                    onDismiss = { showRangeReportDialog = false }
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

fun shareExportedFile(context: android.content.Context, file: java.io.File, format: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = if (format == "JSON") "application/json" else "text/csv"
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, file.name)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Export File"))
        android.widget.Toast.makeText(context, "Export saved: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Export saved, but couldn't open share menu: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ExportDialog(
    title: String,
    scopeOptions: List<String>,
    formatOptions: List<String>,
    onExport: (scope: String, format: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedScope by remember { mutableStateOf(scopeOptions.first()) }
    var selectedFormat by remember { mutableStateOf(formatOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select Scope:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                scopeOptions.forEach { scope ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedScope = scope }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(scope)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Select Format:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                formatOptions.forEach { format ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(format)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(selectedScope, selectedFormat) }) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeReportDialog(
    onGenerate: (startDate: Long, endDate: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectingEnd by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    if (!selectingEnd) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    selectingEnd = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text("Select Start Date", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
                DatePicker(state = datePickerState)
            }
        }
    } else {
        val endDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val end = endDatePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        if (startDate != null && end >= startDate!!) {
                            onGenerate(startDate!!, end)
                        }
                    },
                    enabled = (endDatePickerState.selectedDateMillis ?: 0L) >= (startDate ?: 0L)
                ) { Text("Generate") }
            },
            dismissButton = {
                TextButton(onClick = { selectingEnd = false }) { Text("Back") }
            }
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text("Select End Date", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
                DatePicker(state = endDatePickerState)
            }
        }
    }
}

@Composable
private fun ServiceTierItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    countText: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = iconTint
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SyncResultBanner(result: SyncResult) {
    val containerColor = if (result.success) StatusReady.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f)
    val borderColor = if (result.success) StatusReady.copy(alpha = 0.4f) else ErrorRed.copy(alpha = 0.4f)
    val icon = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error
    val iconTint = if (result.success) StatusReady else ErrorRed

    Surface(
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (result.success) "Sync Complete" else "Sync Issue",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = iconTint
                )
            }

            Text(
                text = result.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (result.success) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultTag(label = "Updated: ${result.jobsUpdatedLocally}")
                    ResultTag(label = "Uploaded: ${result.jobsUploadedToCloud}")
                    ResultTag(label = "Chip-Level: ${result.chipLevelSynced}")
                }
            }
        }
    }
}

@Composable
private fun ResultTag(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
