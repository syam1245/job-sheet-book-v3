package com.example.jobsheetbook.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.jobsheetbook.R
import com.example.jobsheetbook.domain.models.JobStatus
import com.example.jobsheetbook.domain.policy.WarningLevel
import com.example.jobsheetbook.domain.policy.WarningReason
import com.example.jobsheetbook.ui.theme.JobSheetBookTheme
import com.example.jobsheetbook.ui.viewmodel.PendingStatusTransition
import com.example.jobsheetbook.util.StatusUtil

// ── Public composables ────────────────────────────────────────────────────────

@Composable
fun TransitionWarningDialog(
    pendingTransition: PendingStatusTransition,
    displayJobId: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        TransitionWarningContent(
            pendingTransition = pendingTransition,
            displayJobId = displayJobId,
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
}

@Composable
fun TransitionWarningContent(
    pendingTransition: PendingStatusTransition,
    displayJobId: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTerminalToTerminal =
        (pendingTransition.from == JobStatus.DELIVERED || pendingTransition.from == JobStatus.RETURN) &&
        (pendingTransition.to == JobStatus.DELIVERED || pendingTransition.to == JobStatus.RETURN)

    val titleRes = when {
        pendingTransition.level == WarningLevel.STRONG && isTerminalToTerminal -> R.string.warning_title_strong_terminal
        pendingTransition.level == WarningLevel.STRONG -> R.string.warning_title_strong
        else -> R.string.warning_title_soft
    }

    val isReopenToActive = pendingTransition.reasons.contains(WarningReason.LeavingTerminal) &&
            pendingTransition.to != JobStatus.DELIVERED && pendingTransition.to != JobStatus.RETURN

    val confirmLabelRes = if (isReopenToActive) {
        R.string.action_reopen_job
    } else {
        R.string.action_change_anyway
    }

    val isStrong = pendingTransition.level == WarningLevel.STRONG

    Surface(
        modifier = modifier
            .widthIn(max = 360.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header badge ─────────────────────────────────────────────
            DialogIconBadge(isStrong = isStrong)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Title ────────────────────────────────────────────────────
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Subtitle: Job ID ─────────────────────────────────────────
            Text(
                text = stringResource(R.string.warning_subtitle_job_id, displayJobId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── From → To status row ──────────────────────────────────────
            StatusTransitionRow(
                from = pendingTransition.from,
                to = pendingTransition.to
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Reason cards ──────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pendingTransition.reasons.forEach { reason ->
                    ReasonCard(
                        reason = reason,
                        toStatus = pendingTransition.to,
                        isStrong = isStrong
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── "Keep current status" — filled, the safe choice ───────────
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_keep_status),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Confirm / destructive — outlined, below ───────────────────
            val confirmContentColor = if (isStrong) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
            OutlinedButton(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = confirmContentColor
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = confirmContentColor.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(confirmLabelRes),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ── Private sub-composables ───────────────────────────────────────────────────

@Composable
private fun DialogIconBadge(isStrong: Boolean) {
    val badgeColor = if (isStrong) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val icon: ImageVector = if (isStrong) Icons.Default.Error else Icons.Default.Warning

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.12f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun StatusTransitionRow(
    from: JobStatus,
    to: JobStatus
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusPill(
            status = from,
            modifier = Modifier.weight(1f, fill = false)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(20.dp)
        )

        StatusPill(
            status = to,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun StatusPill(
    status: JobStatus,
    modifier: Modifier = Modifier
) {
    val dotColor = StatusUtil.getStatusColor(status.value)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            softWrap = true
        )
    }
}

@Composable
private fun ReasonCard(
    reason: WarningReason,
    toStatus: JobStatus,
    isStrong: Boolean
) {
    val headlineRes: Int
    val detailRes: Int
    val icon: ImageVector
    val isErrorTinted = reason is WarningReason.LeavingTerminal && isStrong

    when (reason) {
        is WarningReason.SkippedStep -> {
            headlineRes = R.string.warning_reason_headline_skipped_step
            detailRes = R.string.warning_reason_skipped_step
            icon = Icons.Default.SkipNext
        }
        is WarningReason.BackwardMove -> {
            headlineRes = R.string.warning_reason_headline_backward_move
            detailRes = R.string.warning_reason_backward_move
            icon = Icons.AutoMirrored.Filled.Undo
        }
        is WarningReason.LeavingTerminal -> {
            headlineRes = R.string.warning_reason_headline_leaving_terminal
            detailRes = if (toStatus == JobStatus.DELIVERED || toStatus == JobStatus.RETURN) {
                R.string.warning_reason_leaving_terminal_archived
            } else {
                R.string.warning_reason_leaving_terminal_active
            }
            icon = Icons.Default.Lock
        }
        is WarningReason.ZeroCost -> {
            headlineRes = R.string.warning_reason_headline_zero_cost
            detailRes = R.string.warning_reason_zero_cost
            icon = Icons.Default.MonetizationOn
        }
    }

    val containerColor = if (isErrorTinted) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val iconTint = if (isErrorTinted) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val headlineColor = if (isErrorTinted) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 1.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(headlineRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = headlineColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(detailRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun PreviewStrongReopen() {
    JobSheetBookTheme {
        TransitionWarningContent(
            pendingTransition = PendingStatusTransition(
                jobId = 1L,
                from = JobStatus.DELIVERED,
                to = JobStatus.READY,
                level = WarningLevel.STRONG,
                reasons = listOf(WarningReason.LeavingTerminal, WarningReason.BackwardMove),
                note = ""
            ),
            displayJobId = "D-00001",
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStrongTerminalToTerminal() {
    JobSheetBookTheme {
        TransitionWarningContent(
            pendingTransition = PendingStatusTransition(
                jobId = 1L,
                from = JobStatus.DELIVERED,
                to = JobStatus.RETURN,
                level = WarningLevel.STRONG,
                reasons = listOf(WarningReason.LeavingTerminal),
                note = ""
            ),
            displayJobId = "D-00002",
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMultiReasonSoft() {
    JobSheetBookTheme {
        TransitionWarningContent(
            pendingTransition = PendingStatusTransition(
                jobId = 1L,
                from = JobStatus.RECEIVED,
                to = JobStatus.READY,
                level = WarningLevel.SOFT,
                reasons = listOf(WarningReason.SkippedStep),
                note = ""
            ),
            displayJobId = "D-00003",
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewStrongReopenDark() {
    JobSheetBookTheme(darkTheme = true) {
        TransitionWarningContent(
            pendingTransition = PendingStatusTransition(
                jobId = 1L,
                from = JobStatus.DELIVERED,
                to = JobStatus.READY,
                level = WarningLevel.STRONG,
                reasons = listOf(WarningReason.LeavingTerminal, WarningReason.BackwardMove),
                note = ""
            ),
            displayJobId = "D-00001",
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAllFourReasons() {
    JobSheetBookTheme {
        TransitionWarningContent(
            pendingTransition = PendingStatusTransition(
                jobId = 2L,
                from = JobStatus.DELIVERED,
                to = JobStatus.IN_PROGRESS,
                level = WarningLevel.STRONG,
                reasons = listOf(
                    WarningReason.LeavingTerminal,
                    WarningReason.BackwardMove,
                    WarningReason.SkippedStep,
                    WarningReason.ZeroCost
                ),
                note = ""
            ),
            displayJobId = "D-00099",
            onConfirm = {},
            onCancel = {}
        )
    }
}

