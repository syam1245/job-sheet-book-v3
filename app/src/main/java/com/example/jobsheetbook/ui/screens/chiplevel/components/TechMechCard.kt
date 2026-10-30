package com.example.jobsheetbook.ui.screens.chiplevel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.example.jobsheetbook.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobsheetbook.domain.models.JobStatus
import com.example.jobsheetbook.ui.viewmodel.ChipLevelJobItem
import com.example.jobsheetbook.util.FollowUpState
import com.example.jobsheetbook.util.TechMechStatusUtil
import java.text.SimpleDateFormat
import java.util.*



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TechMechCard(
    item: ChipLevelJobItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val longClickLabel = stringResource(R.string.techmech_enquiry_long_click_label)
    val colorScheme = MaterialTheme.colorScheme

    val isChipLevel = item.job.status == JobStatus.CHIP_LEVEL ||
            item.job.status == JobStatus.SENT_OUT
    val sinceMillis = item.info.pickedUpAt ?: item.job.createdAt
    val followUpState = TechMechStatusUtil.getFollowUpState(
        isAtTechMech = isChipLevel,
        sinceMillis = sinceMillis
    )

    val outcomeLabel = TechMechStatusUtil.getOutcomeLabel(item.job.status)

    // Status Color per stage / follow-up
    val statusColor = when {
        followUpState == FollowUpState.OVERDUE         -> OverdueRed
        followUpState == FollowUpState.NEEDS_FOLLOW_UP -> FollowUpOrange
        isChipLevel                                    -> AtTechMechAmber
        item.job.status == JobStatus.READY ||
            item.job.status == JobStatus.DELIVERED     -> BackInGreen
        item.job.status == JobStatus.RETURN            -> OverdueRed
        else                                           -> AwaitingGray
    }

    val timeLabel = if (isChipLevel) {
        val days = TechMechStatusUtil.daysSince(sinceMillis)
        if (days == 0) "Today" else "${days}d at TM"
    } else {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        sdf.format(Date(item.job.createdAt))
    }

    val effectiveFinalCost = if (item.info.finalCost > 0) item.info.finalCost else item.job.finalCost
    val profit = if (effectiveFinalCost > 0) effectiveFinalCost - item.info.tmCost else 0
    val needsCharges = !isChipLevel && item.info.tmCost == 0 && effectiveFinalCost == 0

    val dueDateStr = item.job.dueDate?.let {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let { action ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    }
                },
                onLongClickLabel = if (onLongClick != null) longClickLabel else null
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
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

            // Main Content Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // ── Left Column: Core Info & TM Details ──
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.job.displayId,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = com.example.jobsheetbook.ui.theme.LocalSemanticTypography.current.jobId
                    )
                    Text(
                        text = item.job.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.jobsheetbook.ui.theme.LocalSemanticTypography.current.customerName
                    )
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            withStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    color = com.example.jobsheetbook.ui.theme.LocalSemanticTypography.current.brand,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) {
                                append(item.job.make)
                            }
                            if (item.job.model.isNotBlank()) {
                                append(" ${item.job.model}")
                            }
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant
                    )

                    val issueText = if (item.job.remarks.isNotBlank()) item.job.remarks else item.job.jobTypes.joinToString(", ")
                    if (issueText.isNotBlank()) {
                        Text(
                            text = "Issue: $issueText",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // ── Tertiary Tier: TM#, Work Description ──
                    val hasTmDetails = item.info.tmJobId.isNotBlank() || item.info.workDone.isNotBlank()
                    if (hasTmDetails) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (item.info.tmJobId.isNotBlank()) {
                                Text(
                                    text = "TM# ${item.info.tmJobId}",
                                    color = com.example.jobsheetbook.ui.theme.LocalSemanticTypography.current.techMechId,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (item.info.tmJobId.isNotBlank() && item.info.workDone.isNotBlank()) {
                                Text("|", color = colorScheme.outline, fontSize = 12.sp)
                            }
                            if (item.info.workDone.isNotBlank()) {
                                Text(
                                    text = item.info.workDone,
                                    color = colorScheme.outline,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // ── Charges & Profit Row ──
                    if (item.info.tmCost > 0 || effectiveFinalCost > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "TM: ₹${item.info.tmCost}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurfaceVariant
                            )
                            Text("•", color = colorScheme.outline, fontSize = 12.sp)
                            Text(
                                text = "Final: ₹$effectiveFinalCost",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurfaceVariant
                            )
                            if (profit != 0) {
                                Text("•", color = colorScheme.outline, fontSize = 12.sp)
                                Text(
                                    text = "Profit: ₹$profit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profit > 0) BackInGreen else OverdueRed
                                )
                            }
                        }
                    }
                }

                // ── Right Column: Badges ──
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Outcome Badge
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(outcomeLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Enter charges badge
                    if (needsCharges) {
                        Box(
                            modifier = Modifier
                                .background(OverdueRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, OverdueRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Enter charges", color = OverdueRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Time Badge
                    Box(
                        modifier = Modifier
                            .background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(timeLabel, color = colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    if (dueDateStr != null) {
                        // Due Date Badge — use primary color for adaptive readability
                        Box(
                            modifier = Modifier
                                .background(colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Due $dueDateStr",
                                color = colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
