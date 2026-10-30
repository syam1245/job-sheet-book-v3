package com.example.jobsheetbook.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.ui.theme.*
import com.example.jobsheetbook.ui.viewmodel.JobViewModel
import com.example.jobsheetbook.ui.viewmodel.JobPreviewUiState
import com.example.jobsheetbook.util.PhoneNormalizer
import com.example.jobsheetbook.util.StatusUtil
import com.example.jobsheetbook.util.WhatsAppUtil
import java.text.SimpleDateFormat
import java.util.*

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobPreviewScreen(
    jobId: Long,
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    viewModel: JobViewModel = hiltViewModel(),
    chipLevelViewModel: com.example.jobsheetbook.ui.screens.chiplevel.ChipLevelViewModel = hiltViewModel()
) {
    var partsOrderTrigger by remember { mutableStateOf(0) }
    
    val uiState by viewModel.getJobPreviewUiState(jobId, partsOrderTrigger).collectAsState(initial = JobPreviewUiState())
    val job = uiState.job
    val events = uiState.events
    val partsState = uiState.partsState
    val pendingTransition by viewModel.pendingStatusTransition.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val statuses = listOf("Received", "Work in-progress", "Ready", "Chip-Level", "Pending", "Delivered", "Return")
    val isDark = MaterialTheme.colorScheme.background == com.example.jobsheetbook.ui.theme.DeepNavy

    pendingTransition?.let { pending ->
        if (pending.jobId == jobId) {
            com.example.jobsheetbook.ui.screens.components.TransitionWarningDialog(
                pendingTransition = pending,
                displayJobId = job?.displayJobId ?: "",
                onConfirm = { viewModel.confirmStatusTransition() },
                onCancel = { viewModel.cancelStatusTransition() }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Job", fontWeight = FontWeight.Bold) },
            text = { Text("This moves the job to Trash Bin. You can restore it later from Settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteJob(jobId) {
                            onDeleteClick(jobId)
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            job?.displayId ?: "Job #$jobId", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = LocalSemanticTypography.current.jobId
                        )
                        job?.let {
                            Text("${it.make} ${it.model}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(jobId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Job", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Job", tint = MaterialTheme.colorScheme.error)
                    }
                    val whatsAppDigits = PhoneNormalizer.toWhatsAppDigits(job?.customerPhone)
                    if (!job?.customerPhone.isNullOrBlank() && whatsAppDigits != null) {
                        IconButton(onClick = {
                            job?.let {
                                val message = WhatsAppUtil.generateStatusMessage(it)
                                val url = WhatsAppUtil.getWhatsAppIntentUrl(it.customerPhone, message)
                                if (url != null) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = url.toUri()
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "WhatsApp Receipt", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        job?.let { currentJob ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Status Section ──
                SectionCard(icon = Icons.Outlined.Build, title = "Repair Status") {
                    val statusColor = StatusUtil.getStatusColor(currentJob.rawStatus)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(statusColor))
                        Spacer(Modifier.width(10.dp))
                        Text(currentJob.rawStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }

                    if (currentJob.rawStatus == "Pending" && currentJob.pendingReason.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            color = StatusWaiting.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StatusWaiting.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = StatusWaiting, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("On Hold", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = StatusWaiting)
                                    Text(currentJob.pendingReason, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    var expanded by remember { mutableStateOf(false) }
                    var showChipLevelDialog by remember { mutableStateOf(false) }
                    var tmJobId by remember { mutableStateOf("") }
                    var workDone by remember { mutableStateOf("") }
                    var tmCostStr by remember { mutableStateOf("") }
                    var finalCostStr by remember { mutableStateOf(if (currentJob.finalCost > 0) currentJob.finalCost.toString() else "") }

                    val calculatedProfit = remember(tmCostStr, finalCostStr) {
                        val final = finalCostStr.toIntOrNull() ?: 0
                        val tm = tmCostStr.toIntOrNull() ?: 0
                        if (final > 0) final - tm else 0
                    }

                    LaunchedEffect(currentJob.jobId) {
                        viewModel.uiEvents.collect { event ->
                            when (event) {
                                is com.example.jobsheetbook.ui.viewmodel.JobUiEvent.OpenChipLevelDialog -> {
                                    if (event.jobId == currentJob.jobId) {
                                        tmJobId = ""
                                        workDone = ""
                                        tmCostStr = ""
                                        finalCostStr = if (currentJob.finalCost > 0) currentJob.finalCost.toString() else ""
                                        showChipLevelDialog = true
                                    }
                                }
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = currentJob.rawStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Update Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            statuses.forEach { status ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusUtil.getStatusColor(status)))
                                            Spacer(Modifier.width(12.dp))
                                            Text(status)
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateJobStatus(currentJob, status)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (showChipLevelDialog) {
                        AlertDialog(
                            onDismissRequest = { showChipLevelDialog = false },
                            shape = RoundedCornerShape(20.dp),
                            title = { Text("Outsource to Chip-Level", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Enter TechMech tracking details below.")
                                    OutlinedTextField(
                                        value = tmJobId,
                                        onValueChange = { tmJobId = it },
                                        label = { Text("TechMech Job #") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = workDone,
                                        onValueChange = { workDone = it },
                                        label = { Text("Work / Section") },
                                        placeholder = { Text("e.g. IO Section, Display IC") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = tmCostStr,
                                        onValueChange = { tmCostStr = it.filter { char -> char.isDigit() } },
                                        label = { Text("TechMech Cost (₹)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = finalCostStr,
                                        onValueChange = { finalCostStr = it.filter { char -> char.isDigit() } },
                                        label = { Text("Final Cost to Customer (₹)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    if (finalCostStr.isNotBlank()) {
                                        Text("Profit: ₹$calculatedProfit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        chipLevelViewModel.saveChipLevelDetails(
                                            jobId = currentJob.jobId,
                                            tmJobId = tmJobId,
                                            tmCost = tmCostStr.toIntOrNull() ?: 0,
                                            finalCost = finalCostStr.toIntOrNull() ?: 0,
                                            workDone = workDone
                                        )
                                        showChipLevelDialog = false
                                    }
                                ) { Text("Save Details") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showChipLevelDialog = false }) { Text("Skip") }
                            }
                        )
                    }
                }

                if (partsState != null && partsState!!.orderStatus != "Cleared") {
                    PartsOrderCard(
                        partsState = partsState!!,
                        jobId = jobId,
                        itemType = currentJob.itemType,
                        make = currentJob.make,
                        model = currentJob.model,
                        customerName = currentJob.customerName,
                        displayId = currentJob.displayId,
                        onMarkOrdered = { 
                            viewModel.markPartsOrdered(jobId)
                            partsOrderTrigger++
                        },
                        onMarkArrived = { 
                            viewModel.markPartsArrived(jobId)
                            partsOrderTrigger++
                        },
                        onConfirmOrder = { 
                            viewModel.confirmPartsOrder(jobId)
                            partsOrderTrigger++
                        },
                        onSkipOrder = { 
                            viewModel.clearPartsState(jobId)
                            partsOrderTrigger++
                        },
                        context = context
                    )
                }

                // ── Customer Contact ──
                SectionCard(icon = Icons.Outlined.Call, title = "Customer Details") {
                    Text(
                        text = currentJob.customerName, 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = LocalSemanticTypography.current.customerName
                    )
                    if (currentJob.customerPhone.isNotBlank()) {
                        val dialNumber = PhoneNormalizer.toDialNumber(currentJob.customerPhone)
                        Spacer(Modifier.height(8.dp))
                        val phoneModifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (dialNumber != null) {
                                    Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_DIAL).apply { data = "tel:$dialNumber".toUri() }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: android.content.ActivityNotFoundException) {
                                            android.widget.Toast.makeText(context, "No dialer app found on this device", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else Modifier
                            )
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)

                        Text(
                            text = PhoneNormalizer.formatForDisplay(currentJob.customerPhone),
                            modifier = phoneModifier,
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.SemiBold, 
                            color = LocalSemanticTypography.current.phone
                        )
                    }
                }

                // ── Device Details ──
                SectionCard(icon = Icons.Outlined.Devices, title = "Device Details") {
                    DetailRow("Type", currentJob.itemType)
                    DetailRow("Make", currentJob.make, valueColor = LocalSemanticTypography.current.brand)
                    DetailRow("Model", currentJob.model)
                    DetailRow("Serial", currentJob.serialNumber)
                    DetailRow("Password", currentJob.devicePassword)
                    val isFinished = currentJob.rawStatus.equals("Delivered", ignoreCase = true) || currentJob.rawStatus.startsWith("Return", ignoreCase = true)
                    if (currentJob.dueDate != null && !isFinished) {
                        val status = viewModel.getDueDateStatus(currentJob.dueDate)
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(currentJob.dueDate))
                        val (dueText, dueColor) = when (status) {
                            com.example.jobsheetbook.ui.viewmodel.DueDateStatus.OVERDUE -> "Overdue ($dateStr)" to Color(0xFFEF4444)
                            com.example.jobsheetbook.ui.viewmodel.DueDateStatus.TODAY -> "Today ($dateStr)" to Color(0xFFF97316)
                            com.example.jobsheetbook.ui.viewmodel.DueDateStatus.URGENT -> "Urgent ($dateStr)" to Color(0xFFEAB308)
                            else -> dateStr to (if (isDark) DateTimeDark else DateTimeLight)
                        }
                        DetailRow("Target Due Date", dueText, valueColor = dueColor)
                    }

                    if (currentJob.estimatedCost > 0) {
                        DetailRow("Estimated Cost", "₹${currentJob.estimatedCost}")
                    }
                    if (currentJob.finalCost > 0) {
                        DetailRow("Final Cost", "₹${currentJob.finalCost}")
                    }
                }

                // ── Job Types ──
                if (currentJob.jobTypes.isNotEmpty()) {
                    SectionCard(icon = Icons.AutoMirrored.Outlined.Assignment, title = "Job Type Tags") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentJob.jobTypes.forEach { type ->
                                SuggestionChip(onClick = {}, label = { Text(type) })
                            }
                        }
                    }
                }

                // ── Accessories ──
                if (currentJob.accessories.isNotEmpty() && currentJob.accessories.any { it.value }) {
                    SectionCard(icon = Icons.Outlined.Inventory2, title = "Accessories") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentJob.accessories.filter { it.value }.forEach { (key, _) ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = key.replaceFirstChar { it.uppercase() }, 
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), 
                                        style = MaterialTheme.typography.labelMedium, 
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Remarks ──
                if (currentJob.remarks.isNotBlank()) {
                    SectionCard(icon = Icons.AutoMirrored.Outlined.Notes, title = "Remarks") {
                        Text(currentJob.remarks, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── History Timeline ──
                SectionCard(icon = Icons.Outlined.History, title = "History Log") {
                    if (events.isEmpty()) {
                        Text("No history recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    } else {
                        events.forEachIndexed { index, event ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (event.eventType == "STATUS_CHANGE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary))
                                    if (index < events.size - 1) {
                                        Box(modifier = Modifier.width(1.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(event.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(event.timestamp)), 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = if (isDark) DateTimeDark else DateTimeLight
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Created Info ──
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Created", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Text(
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(currentJob.createdAt)), 
                                style = MaterialTheme.typography.bodyMedium, 
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) DateTimeDark else DateTimeLight
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Text(
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(currentJob.createdAt)), 
                                style = MaterialTheme.typography.bodyMedium, 
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) DateTimeDark else DateTimeLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
        }
    }
}

@Composable
private fun SectionCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    if (value.isNotBlank()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 2.dp))
    }
}

@Composable
private fun PartsOrderCard(
    partsState: com.example.jobsheetbook.data.repository.PartsOrderState,
    jobId: Long,
    itemType: String = "",
    make: String,
    model: String,
    customerName: String,
    displayId: String,
    onMarkOrdered: () -> Unit,
    onMarkArrived: () -> Unit,
    onConfirmOrder: () -> Unit,
    onSkipOrder: () -> Unit,
    context: android.content.Context
) {
    if (partsState.orderStatus == "Pending" && !partsState.confirmedOrder) {
        SectionCard(icon = Icons.Outlined.Build, title = "🔧 Parts Detected") {
            Text("The following parts were mentioned in this job's remarks:\n${partsState.detectedParts.joinToString(", ")}\nDo you need to ORDER these parts, or just noted for reference?", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirmOrder, modifier = Modifier.weight(1f)) {
                    Text("Yes, I Need to Order")
                }
                OutlinedButton(onClick = onSkipOrder, modifier = Modifier.weight(1f)) {
                    Text("Just a Note, Skip")
                }
            }
        }
    } else {
        SectionCard(icon = Icons.Outlined.Build, title = "Parts Required") {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                partsState.detectedParts.forEach { part ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = part.replaceFirstChar { it.uppercase() }, 
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), 
                            style = MaterialTheme.typography.labelMedium, 
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            val (statusText, statusColor) = when (partsState.orderStatus) {
                "Pending" -> "Pending" to Color(0xFFFFA000) // Amber
                "Ordered" -> "Ordered" to Color(0xFF1976D2) // Blue
                "Arrived" -> "Arrived" to Color(0xFF388E3C) // Green
                else -> partsState.orderStatus to Color.Gray
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(6.dp))
                Text(statusText, style = MaterialTheme.typography.bodyMedium, color = statusColor, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(12.dp))
            
            val deviceSummary = WhatsAppUtil.formatDeviceSummary(itemType, make, model)
            val whatsappMessage = """
Parts Order Request:
Job #$displayId | $customerName
Device: $deviceSummary
Parts needed: ${partsState.detectedParts.joinToString(", ")}
            """.trimIndent()
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (partsState.orderStatus) {
                    "Pending" -> {
                        // Track whether the confirmation dialog is open
                        var showOrderConfirm by remember { mutableStateOf(false) }

                        if (showOrderConfirm) {
                            AlertDialog(
                                onDismissRequest = { showOrderConfirm = false },
                                title = { Text("Confirm Parts Order") },
                                text = {
                                    Text(
                                        "Mark this parts order as placed and open WhatsApp to send the order message?\n\n" +
                                        "Parts: ${partsState.detectedParts.joinToString(", ")}"
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        showOrderConfirm = false
                                        // Only commit the state change after user confirms
                                        onMarkOrdered()
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("https://wa.me/?text=" + android.net.Uri.encode(whatsappMessage))
                                        }
                                        context.startActivity(intent)
                                    }) {
                                        Text("Yes, Mark as Ordered")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { showOrderConfirm = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        Button(
                            onClick = { showOrderConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark as Ordered")
                        }
                        OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.weight(1f)) {
                            Text("Mark as Arrived")
                        }
                    }
                    "Ordered" -> {
                        Button(onClick = { }, enabled = false, modifier = Modifier.weight(1f)) {
                            Text("✓ Ordered")
                        }
                        Button(onClick = onMarkArrived, modifier = Modifier.weight(1f)) {
                            Text("Mark as Arrived")
                        }
                    }
                    "Arrived" -> {
                        Text("✓ Parts Arrived", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}
