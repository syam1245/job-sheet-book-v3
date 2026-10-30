package com.example.jobsheetbook.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.domain.models.JobStatus
import com.example.jobsheetbook.ui.theme.*
import com.example.jobsheetbook.ui.viewmodel.JobViewModel
import com.example.jobsheetbook.util.PhoneNormalizer
import com.example.jobsheetbook.util.StatusUtil
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.example.jobsheetbook.R
import com.example.jobsheetbook.ui.screens.chiplevel.components.TechMechEnquirySheet
import com.example.jobsheetbook.ui.screens.chiplevel.components.rememberTechMechEnquiryLabels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onJobClick: (Long) -> Unit,
    onNewJobClick: () -> Unit,
    viewModel: JobViewModel = hiltViewModel()
) {
    val uiState by viewModel.dashboardUiState.collectAsState()
    val enquiryLabels = rememberTechMechEnquiryLabels()
    val enquiryState by viewModel.techMechEnquiryState.collectAsState()
    
    val totalJobsCount = uiState.totalJobsCount
    val ready = uiState.readyCount
    val inProgressCount = uiState.inProgressCount
    val chipLevelCount = uiState.activeChipLevelCount
    val intakeCount = uiState.receivedCount
    val urgentJobCount = uiState.urgentJobCount
    val returnCount = uiState.returnCount
    val deliveredCount = uiState.deliveredCount

    val searchQuery = uiState.searchQuery
    val selectedStatusFilter = uiState.selectedStatusFilter

    val currentJobs = uiState.currentJobs
    val filteredJobsCount = uiState.filteredJobsCount

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = onNewJobClick,
                    containerColor = DashboardBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Job",
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "New Job",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── Top App Bar ──
            item {
                TopBarSection(
                    onReportClick = {
                        viewModel.generateAndCopyDailyReport(context) {
                            // Save to SharedPreferences
                            val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                            context.getSharedPreferences("report_prefs", android.content.Context.MODE_PRIVATE)
                                .edit()
                                .putString("last_report_date", sdf.format(java.util.Date()))
                                .apply()
                            
                            // Show Snackbar
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Report copied to clipboard!")
                            }
                        }
                    }
                )
            }

            // ── Status Dashboard Row ──
            item {
                StatusDashboardRow(
                    total = totalJobsCount,
                    received = intakeCount,
                    inProgress = inProgressCount,
                    ready = ready,
                    chipLevel = chipLevelCount,
                    selectedStatusFilter = selectedStatusFilter,
                    onStatusSelected = { viewModel.onStatusFilterChange(it) }
                )
            }

            // ── Search & Filter Section ──
            item {
                SearchBarSection(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) }
                )
            }

            item {
                val counts = remember(totalJobsCount, uiState.newestJobsCount, uiState.oldJobsCount, intakeCount, inProgressCount, ready, chipLevelCount, urgentJobCount, returnCount, deliveredCount) {
                    mapOf(
                        null to totalJobsCount,
                        "Newest" to uiState.newestJobsCount,
                        "Received" to intakeCount,
                        "Work in-progress" to inProgressCount,
                        "Ready" to ready,
                        "Chip-Level" to chipLevelCount,
                        "Urgent" to urgentJobCount,
                        "Return" to returnCount,
                        "Delivered" to deliveredCount,
                        "Old-Jobs" to uiState.oldJobsCount
                    )
                }
                FilterChipsRow(
                    selectedStatus = selectedStatusFilter,
                    onStatusSelected = { viewModel.onStatusFilterChange(it) },
                    counts = counts
                )
            }

            // ── List Header ──
            item {
                JobListHeader(isSearching = searchQuery.isNotBlank())
            }

            // ── Job List ──
            if (currentJobs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("No jobs found", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(
                    items = currentJobs,
                    key = { job -> job.jobId }
                ) { job ->
                    JobCard(
                        job = job,
                        onClick = { onJobClick(job.jobId) },
                        onLongClick = { viewModel.onJobCardLongClick(job, enquiryLabels) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                if (currentJobs.size < filteredJobsCount) {
                    item(key = "load_more_button") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { viewModel.loadMoreJobs() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DashboardBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Load More Jobs (${currentJobs.size} of $filteredJobsCount)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    enquiryState?.let { state ->
        TechMechEnquirySheet(
            job = state.job,
            message = state.message,
            onDismiss = { viewModel.dismissTechMechEnquiry() }
        )
    }
}

// ═══════════════════════════════════════════════════════
//  TOP APP BAR
// ═══════════════════════════════════════════════════════

@Composable
private fun TopBarSection(onReportClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "JOB SHEET BOOK",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Admin info solution",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = DashboardBlue
            )
        }
        
        IconButton(
            onClick = onReportClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = "Daily Report",
                tint = DashboardBlue,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
//  STATUS DASHBOARD ROW
// ═══════════════════════════════════════════════════════

@Composable
private fun StatusDashboardRow(
    received: Int,
    inProgress: Int,
    ready: Int,
    chipLevel: Int,
    total: Int = 0,
    selectedStatusFilter: String? = null,
    onStatusSelected: (String?) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(IntrinsicSize.Max)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // For "Total" card
        Surface(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .heightIn(min = 68.dp)
                .clickable { 
                    onStatusSelected(null)  // null = show all jobs
                },
            shape = RoundedCornerShape(10.dp),
            border = if (selectedStatusFilter == null) 
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = if (selectedStatusFilter == null)
                MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
        ) { 
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    ),
                    color = if (selectedStatusFilter == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (selectedStatusFilter == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }

        // For "Received" card
        val isReceivedSelected = selectedStatusFilter == "Received"
        Surface(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .heightIn(min = 68.dp)
                .clickable { 
                    onStatusSelected("Received") 
                },
            shape = RoundedCornerShape(10.dp),
            border = if (isReceivedSelected) 
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = if (isReceivedSelected)
                MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
        ) { 
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RECEIVED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    ),
                    color = if (isReceivedSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = received.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isReceivedSelected) MaterialTheme.colorScheme.primary else StatusReceived,
                    maxLines = 1
                )
            }
        }

        // For "In Progress" card
        val isInProgressSelected = selectedStatusFilter == "Work in-progress" || selectedStatusFilter == "In Progress"
        Surface(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .heightIn(min = 68.dp)
                .clickable { 
                    onStatusSelected("Work in-progress") 
                },
            shape = RoundedCornerShape(10.dp),
            border = if (isInProgressSelected) 
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = if (isInProgressSelected)
                MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
        ) { 
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "IN PROGRESS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    ),
                    color = if (isInProgressSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = inProgress.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isInProgressSelected) MaterialTheme.colorScheme.primary else StatusInProgressBlue,
                    maxLines = 1
                )
            }
        }

        // For "Ready" card
        Surface(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .heightIn(min = 68.dp)
                .clickable { 
                    onStatusSelected("Ready") 
                },
            shape = RoundedCornerShape(10.dp),
            border = if (selectedStatusFilter == "Ready") 
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = if (selectedStatusFilter == "Ready")
                MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
        ) { 
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "READY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    ),
                    color = if (selectedStatusFilter == "Ready") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ready.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (selectedStatusFilter == "Ready") MaterialTheme.colorScheme.primary else StatusReady,
                    maxLines = 1
                )
            }
        }

        // For "Chip-Level" card
        Surface(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .heightIn(min = 68.dp)
                .clickable { 
                    onStatusSelected("Chip-Level") 
                },
            shape = RoundedCornerShape(10.dp),
            border = if (selectedStatusFilter == "Chip-Level") 
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = if (selectedStatusFilter == "Chip-Level")
                MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
        ) { 
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CHIP-LEVEL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    ),
                    color = if (selectedStatusFilter == "Chip-Level") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = chipLevel.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (selectedStatusFilter == "Chip-Level") MaterialTheme.colorScheme.primary else StatusDiagnosing,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatusWidget(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════
//  SEARCH BAR SECTION
// ═══════════════════════════════════════════════════════

@Composable
private fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Search jobs (ID, name, phone, brand, issue...)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = DashboardBlue,
                cursorColor = DashboardBlue,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

    }
}

// ═══════════════════════════════════════════════════════
//  FILTER CHIPS ROW
// ═══════════════════════════════════════════════════════

@Composable
private fun FilterChipsRow(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit,
    counts: Map<String?, Int>
) {
    val filters = listOf(
        "Newest" to "Newest",
        "Ready" to "Ready",
        "All" to null,
        "Urgent" to "Urgent",
        "In-progress" to "Work in-progress",
        "Received" to "Received",
        "Chip-Level" to "Chip-Level",
        "Return" to "Return",
        "Delivered" to "Delivered",
        "Old-Jobs" to "Old-Jobs"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (label, statusValue) ->
            val isSelected = selectedStatus == statusValue
            val count = counts[statusValue]

            Surface(
                onClick = { onStatusSelected(statusValue) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) DashboardBlue else Color.Transparent,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (count != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  JOB LIST HEADER
// ═══════════════════════════════════════════════════════

@Composable
private fun JobListHeader(isSearching: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (isSearching) "Search Results — All Stages" else "Jobs",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ═══════════════════════════════════════════════════════
//  JOB CARD
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val date = remember(job.createdAt) {
        val instant = java.time.Instant.ofEpochMilli(job.createdAt)
        val zdt = instant.atZone(java.time.ZoneId.systemDefault())
        java.time.format.DateTimeFormatter.ofPattern("d MMM, yyyy").format(zdt)
    }

    val statusColor = StatusUtil.getStatusColor(job.rawStatus)

    val statusDisplayText = when (job.rawStatus) {
        "Received" -> "Received"
        "Work in-progress" -> "In Progress"
        "Ready" -> "Ready"
        "Chip-Level" -> "Chip-Level"
        "Pending" -> "Pending"
        "Delivered" -> "Delivered"
        "Return" -> "Return"
        else -> job.rawStatus
    }

    val isReady = job.rawStatus == "Ready"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            width = if (isReady) 1.5.dp else 1.dp,
            color = if (isReady) StatusReady.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let { action ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    }
                },
                onLongClickLabel = stringResource(R.string.techmech_enquiry_long_click_label)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left color stripe
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.1f),
                                statusColor,
                                statusColor.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
            )

            // Main Content Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // ── Left Column: Core Info ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = job.displayId,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        color = LocalSemanticTypography.current.jobId
                    )
                    Text(
                        text = job.customerName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = LocalSemanticTypography.current.customerName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Device Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = StatusUtil.getDeviceIcon(job.itemType),
                            contentDescription = null,
                            tint = LocalSemanticTypography.current.brand,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        color = LocalSemanticTypography.current.brand,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                ) {
                                    append(job.make)
                                }
                                if (job.model.isNotBlank()) {
                                    withStyle(
                                        androidx.compose.ui.text.SpanStyle(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        append(" ${job.model}")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Issue / Remarks
                    if (job.remarks.isNotBlank()) {
                        Text(
                            text = job.remarks,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Phone and Chevron
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (job.customerPhone.isNotBlank()) {
                            val dialNumber = PhoneNormalizer.toDialNumber(job.customerPhone)
                            if (dialNumber != null) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Call",
                                    tint = LocalSemanticTypography.current.phone,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:$dialNumber")
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: android.content.ActivityNotFoundException) {
                                                android.widget.Toast.makeText(context, "No dialer app found on this device", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                )
                                Spacer(Modifier.width(2.dp))
                            }
                            Text(
                                text = PhoneNormalizer.formatForDisplay(job.customerPhone),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = LocalSemanticTypography.current.phone
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // ── Right Column: Badges ──
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusDisplayText,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }

                    if (job.isDeleted || job.isArchived) {
                        Text(
                            text = if (job.isDeleted) "Deleted record" else "Archived",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    // Date Badge
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = date,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp)
                        )
                    }

                    // Age Badge
                    val ageText = getAgeBadgeText(job.createdAt)
                    val ageColor = getAgeBadgeColor(job.createdAt)
                    Box(
                        modifier = Modifier
                            .background(ageColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, ageColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = ageText,
                            color = ageColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }

                    // Due Date Badge
                    val isFinished = job.rawStatus.equals("Delivered", ignoreCase = true) || job.rawStatus.startsWith("Return", ignoreCase = true)
                    if (job.dueDate != null && !isFinished) {
                        val calDue = java.util.Calendar.getInstance().apply { timeInMillis = job.dueDate!!; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
                        val calToday = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
                        val daysUntilDue = (calDue.timeInMillis - calToday.timeInMillis) / (24*60*60*1000L)
                        
                        val (badgeColor, badgeText) = when {
                            daysUntilDue < 0  -> Color(0xFFEF4444) to "Overdue ${-daysUntilDue}d"
                            daysUntilDue == 0L -> Color(0xFFF97316) to "Due Today"
                            daysUntilDue == 1L -> Color(0xFFEAB308) to "Due Tmrw"
                            daysUntilDue in 2..3 -> Color(0xFFEAB308) to "Due in ${daysUntilDue}d"
                            else -> Color(0xFF64748B) to "Due in ${daysUntilDue}d"
                        }

                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getAgeBadgeText(createdAt: Long): String {
    val diffMs = System.currentTimeMillis() - createdAt
    val days = (diffMs / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    
    return when {
        days <= 1 -> "TODAY"
        days in 2..6 -> "${days}d"
        days in 7..13 -> "${days}d"
        days in 14..29 -> "${days}d"
        days in 30..89 -> "${days / 7}w"
        else -> "TOO OLD"
    }
}

private fun getAgeBadgeColor(createdAt: Long): Color {
    val diffMs = System.currentTimeMillis() - createdAt
    val days = (diffMs / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)

    return when {
        days <= 6   -> Color(0xFF10B981) // Green
        days in 7..13  -> Color(0xFFF59E0B) // Amber
        days in 14..89 -> Color(0xFFEF4444) // Red
        else           -> Color(0xFF6366F1) // Indigo — visible on both light and dark
    }
}
