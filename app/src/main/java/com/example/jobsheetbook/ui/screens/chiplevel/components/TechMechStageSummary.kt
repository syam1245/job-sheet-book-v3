package com.example.jobsheetbook.ui.screens.chiplevel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobsheetbook.ui.viewmodel.TechMechPipelineSummary

@Composable
fun TechMechStageSummary(pipeline: TechMechPipelineSummary) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StageCard(
                modifier = Modifier.weight(1f),
                count = pipeline.atTechMech,
                label = "At TechMech",
                iconEmoji = "🚚",
                accentColor = AtTechMechAmber
            )
            StageCard(
                modifier = Modifier.weight(1f),
                count = pipeline.backInShop,
                label = "Back from TechMech",
                iconEmoji = "🏬",
                accentColor = BackInGreen
            )
        }

        if (pipeline.needsFollowUp > 0 || pipeline.overdue > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (pipeline.needsFollowUp > 0) {
                    Text(
                        text = "⚠️ ${pipeline.needsFollowUp} Needs Follow-Up",
                        color = FollowUpOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (pipeline.overdue > 0) {
                    Text(
                        text = "🚨 ${pipeline.overdue} Overdue",
                        color = OverdueRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StageCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    iconEmoji: String,
    accentColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(iconEmoji, fontSize = 14.sp)
            }

            Column {
                // Count
                Text(
                    text = count.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    lineHeight = 18.sp
                )

                // Label
                Text(
                    text = label.replace("\n", " "),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
