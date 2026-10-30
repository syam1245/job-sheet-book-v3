package com.example.jobsheetbook.ui.screens

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jobsheetbook.ui.screens.chiplevel.ChipLevelViewModel
import com.example.jobsheetbook.ui.screens.chiplevel.components.*
import com.example.jobsheetbook.ui.viewmodel.ChipLevelJobItem
import com.example.jobsheetbook.ui.viewmodel.TechMechPipelineSummary
import com.example.jobsheetbook.util.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipLevelTrackerScreen(
    onBack: () -> Unit,
    onNavigateToJob: (Long) -> Unit,
    viewModel: ChipLevelViewModel = hiltViewModel()
) {
    val jobs by viewModel.chipLevelJobsFlow.collectAsState()
    val pipeline by viewModel.techMechPipelineSummary.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()
    val enquiryState by viewModel.techMechEnquiryState.collectAsState()
    val enquiryLabels = rememberTechMechEnquiryLabels()

    var selectedJobForDetails by remember { mutableStateOf<ChipLevelJobItem?>(null) }
    var showSearch by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.background)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                        Column {
                            Text(
                                "TechMech Tracker",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                "Chip-level pipeline",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                        val context = LocalContext.current
                        val coroutineScope = rememberCoroutineScope()
                        IconButton(onClick = {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val file = PdfGenerator.generateChipLevelReportPdf(context, jobs)
                                if (file != null) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(intent, "Open TechMech Report"))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Inventory, // Document/Report icon
                                contentDescription = "Generate PDF Report",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Single Compact Summary Strip ──
            item {
                TechMechStageSummary(pipeline = pipeline)
            }

            // ── Filter Pills Row ──
            item {
                FilterPillsRow(
                    pipeline = pipeline,
                    currentFilter = currentFilter,
                    onFilterChange = viewModel::onFilterChange
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Search Bar ──
            item {
                if (showSearch) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier = Modifier.weight(1f).height(50.dp),
                            placeholder = {
                                Text(
                                    "Search jobs...",
                                    fontSize = 13.sp,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colorScheme.surfaceVariant,
                                unfocusedContainerColor = colorScheme.surfaceVariant,
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outlineVariant,
                                focusedTextColor = colorScheme.onSurface,
                                unfocusedTextColor = colorScheme.onSurface
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Job Cards ──
            if (jobs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No jobs found",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(jobs, key = { it.job.jobId }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        TechMechCard(
                            item = item,
                            onClick = { selectedJobForDetails = item },
                            onLongClick = { viewModel.onJobCardLongClick(item.job, enquiryLabels) }
                        )
                    }
                }
            }
        }
    }

    // ── Unified Details Sheet ──
    selectedJobForDetails?.let { item ->
        TechMechDetailsSheet(
            item = item,
            onDismiss = { selectedJobForDetails = null },
            onSaveDetails = { tmJobId, tmCost, finalCost, workDone ->
                viewModel.saveChipLevelDetails(item.job.jobId, tmJobId, tmCost, finalCost, workDone)
            }
        )
    }

    // ── TechMech Enquiry Bottom Sheet ──
    enquiryState?.let { state ->
        TechMechEnquirySheet(
            state = state,
            onDismiss = { viewModel.dismissTechMechEnquiry() }
        )
    }
}

@Composable
private fun FilterPillsRow(
    pipeline: TechMechPipelineSummary,
    currentFilter: String,
    onFilterChange: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val filterItems = listOf(
        Triple("All",                pipeline.atTechMech + pipeline.backInShop, colorScheme.primary),
        Triple("At TechMech",        pipeline.atTechMech,                       AtTechMechAmber),
        Triple("Need follow-up",     pipeline.needsFollowUp,                    FollowUpOrange),
        Triple("Overdue",            pipeline.overdue,                          OverdueRed),
        Triple("Back from TechMech", pipeline.backInShop,                       BackInGreen),
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filterItems) { (label, count, accentColor) ->
            val isSelected = currentFilter.equals(label, ignoreCase = true)
            val bgColor    = if (isSelected) accentColor.copy(alpha = 0.15f) else colorScheme.surfaceVariant
            val borderColor = if (isSelected) accentColor else colorScheme.outlineVariant
            val textColor  = if (isSelected) accentColor else colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable { onFilterChange(label) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) accentColor else colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
