package com.example.jobsheetbook.ui.screens.chiplevel.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobsheetbook.ui.viewmodel.ChipLevelJobItem
import com.example.jobsheetbook.util.PhoneNormalizer
import com.example.jobsheetbook.util.TechMechStatusUtil
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

// Primary action color — intentionally fixed; readable on both light and dark sheets
private val BluePrimary = Color(0xFF2563EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechMechDetailsSheet(
    item: ChipLevelJobItem,
    onDismiss: () -> Unit,
    onSaveDetails: (tmJobId: String, tmCost: Int, finalCost: Int, workDone: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme

    val initialFinalCost = if (item.info.finalCost > 0) item.info.finalCost else item.job.finalCost
    var isEditing by remember { mutableStateOf(false) }
    var tmJobId by remember { mutableStateOf(item.info.tmJobId) }
    var workDone by remember { mutableStateOf(item.info.workDone) }
    var tmCost by remember { mutableStateOf(if (item.info.tmCost > 0) item.info.tmCost.toString() else "") }
    var finalCost by remember { mutableStateOf(if (initialFinalCost > 0) initialFinalCost.toString() else "") }

    val isChipLevel = item.job.status == com.example.jobsheetbook.domain.models.JobStatus.CHIP_LEVEL ||
            item.job.status == com.example.jobsheetbook.domain.models.JobStatus.SENT_OUT
    val sinceMillis = item.info.pickedUpAt ?: item.job.createdAt
    val daysSince = TechMechStatusUtil.daysSince(sinceMillis)

    val outcomeLabel = TechMechStatusUtil.getOutcomeLabel(item.job.status)
    val statusColor = when (item.job.status) {
        com.example.jobsheetbook.domain.models.JobStatus.CHIP_LEVEL,
        com.example.jobsheetbook.domain.models.JobStatus.SENT_OUT -> AtTechMechAmber
        com.example.jobsheetbook.domain.models.JobStatus.READY,
        com.example.jobsheetbook.domain.models.JobStatus.DELIVERED -> BackInGreen
        com.example.jobsheetbook.domain.models.JobStatus.RETURN -> OverdueRed
        else -> AwaitingGray
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colorScheme.outlineVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header: Job & Customer ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.job.displayId,
                        fontSize = 14.sp,
                        color = com.example.jobsheetbook.ui.theme.LocalSemanticTypography.current.jobId,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            withStyle(
                                androidx.compose.ui.text.SpanStyle(color = com.example.jobsheetbook.ui.theme.LocalSemanticTypography.current.customerName)
                            ) {
                                append(item.job.customerName)
                                append(" • ")
                            }
                            withStyle(
                                androidx.compose.ui.text.SpanStyle(color = com.example.jobsheetbook.ui.theme.LocalSemanticTypography.current.brand)
                            ) {
                                append(item.job.make)
                            }
                            if (item.job.model.isNotBlank()) {
                                withStyle(
                                    androidx.compose.ui.text.SpanStyle(color = colorScheme.onSurface)
                                ) {
                                    append(" ${item.job.model}")
                                }
                            }
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp
                    )
                }
                val dialNumber = PhoneNormalizer.toDialNumber(item.job.customerPhone)
                if (item.job.customerPhone.isNotBlank() && dialNumber != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialNumber"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    android.widget.Toast.makeText(context, "No dialer app found on this device", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📞", fontSize = 18.sp)
                    }
                }
            }

            // ── Unified Status & Details Card ──
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // Left Status Color Bar (Gradient)
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

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Status Banner Header (merged into card)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(statusColor.copy(alpha = 0.1f))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    outcomeLabel,
                                    color = statusColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isChipLevel) {
                                    Text(
                                        if (daysSince == 0) "At TechMech since today" else "At TechMech for $daysSince days",
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            item.job.dueDate?.let {
                                val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Due Date", color = colorScheme.outline, fontSize = 11.sp)
                                    Text(
                                        dateStr,
                                        color = colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = colorScheme.outlineVariant)

                        // TM Details Section
                        if (isEditing) {
                            TechMechEditForm(
                                item = item,
                                tmJobId = tmJobId,
                                onTmJobIdChange = { tmJobId = it },
                                workDone = workDone,
                                onWorkDoneChange = { workDone = it },
                                tmCost = tmCost,
                                onTmCostChange = { tmCost = it },
                                finalCost = finalCost,
                                onFinalCostChange = { finalCost = it },
                                onCancel = { isEditing = false },
                                onSave = {
                                    onSaveDetails(tmJobId, tmCost.toIntOrNull() ?: 0, finalCost.toIntOrNull() ?: 0, workDone)
                                    isEditing = false
                                }
                            )
                        } else {
                            TechMechInfoView(
                                item = item,
                                onEditClick = { isEditing = true }
                            )
                        }
                    } // End of Column
                } // End of Row
            } // End of Card
        }
    }
}

@Composable
private fun TechMechEditForm(
    item: ChipLevelJobItem,
    tmJobId: String,
    onTmJobIdChange: (String) -> Unit,
    workDone: String,
    onWorkDoneChange: (String) -> Unit,
    tmCost: String,
    onTmCostChange: (String) -> Unit,
    finalCost: String,
    onFinalCostChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val currentTmCost = tmCost.toIntOrNull() ?: 0
    val currentFinalCost = finalCost.toIntOrNull() ?: 0
    val isFieldChanged = tmJobId != item.info.tmJobId ||
            workDone != item.info.workDone ||
            currentTmCost != item.info.tmCost ||
            currentFinalCost != item.info.finalCost
    val isFinalCostMismatch = currentFinalCost != item.job.finalCost
    val isSaveEnabled = isFieldChanged || isFinalCostMismatch

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Edit Details", fontWeight = FontWeight.Bold, color = colorScheme.onSurface)

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = colorScheme.outlineVariant,
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            focusedLabelColor = BluePrimary,
            unfocusedLabelColor = colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = tmJobId, onValueChange = onTmJobIdChange,
            label = { Text("TM Job #") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )
        OutlinedTextField(
            value = workDone, onValueChange = onWorkDoneChange,
            label = { Text("Work / Section") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = tmCost,
                onValueChange = { onTmCostChange(it.filter { c -> c.isDigit() }) },
                label = { Text("TM Cost (₹)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = fieldColors
            )
            OutlinedTextField(
                value = finalCost,
                onValueChange = { onFinalCostChange(it.filter { c -> c.isDigit() }) },
                label = { Text("Final Cost (₹)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = fieldColors
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onSave,
                enabled = isSaveEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BluePrimary,
                    disabledContainerColor = colorScheme.surfaceVariant
                )
            ) {
                Text("Save", color = if (isSaveEnabled) Color.White else colorScheme.outline)
            }
        }
    }
}

@Composable
private fun TechMechInfoView(
    item: ChipLevelJobItem,
    onEditClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TM Job# ${item.info.tmJobId.ifBlank { "—" }}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.jobsheetbook.ui.theme.LocalSemanticTypography.current.techMechId
            )
            Text(
                "Edit",
                fontSize = 13.sp,
                color = BluePrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onEditClick() }.padding(4.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.info.workDone.ifBlank { "No work description added." },
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("TM Cost", fontSize = 11.sp, color = colorScheme.outline)
                Text(
                    if (item.info.tmCost > 0) "₹${item.info.tmCost}" else "—",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text("Final Cost", fontSize = 11.sp, color = colorScheme.outline)
                Text(
                    if (item.info.finalCost > 0) "₹${item.info.finalCost}" else "—",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Profit", fontSize = 11.sp, color = colorScheme.outline)
                Text(
                    if (item.info.profit > 0) "₹${item.info.profit}" else "—",
                    fontSize = 16.sp,
                    color = BackInGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
