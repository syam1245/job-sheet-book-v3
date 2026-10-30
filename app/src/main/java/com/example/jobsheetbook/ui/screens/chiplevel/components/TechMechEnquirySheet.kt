package com.example.jobsheetbook.ui.screens.chiplevel.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobsheetbook.R
import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.util.TechMechContact
import com.example.jobsheetbook.util.TechMechEnquiryMessage
import com.example.jobsheetbook.util.WhatsAppUtil

data class TechMechEnquiryState(
    val job: Job,
    val message: String
)

@Composable
fun rememberTechMechEnquiryLabels(): TechMechEnquiryMessage.Labels {
    val jobNo = stringResource(R.string.techmech_msg_job_no)
    val customer = stringResource(R.string.techmech_msg_customer)
    val device = stringResource(R.string.techmech_msg_device)
    val problem = stringResource(R.string.techmech_msg_problem)
    val ref = stringResource(R.string.techmech_msg_ref)
    val closing = stringResource(R.string.techmech_msg_closing)
    val notRecorded = stringResource(R.string.techmech_msg_not_recorded)
    val walkIn = stringResource(R.string.techmech_msg_walk_in)
    return remember(jobNo, customer, device, problem, ref, closing, notRecorded, walkIn) {
        TechMechEnquiryMessage.Labels(
            jobNo = jobNo,
            customer = customer,
            device = device,
            problem = problem,
            techMechRef = ref,
            closing = closing,
            notRecorded = notRecorded,
            walkIn = walkIn
        )
    }
}

fun launchWhatsAppShare(context: Context, message: String) {
    val pm = context.packageManager
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    val regularInstalled = isPackageInstalled("com.whatsapp")
    val businessInstalled = isPackageInstalled("com.whatsapp.w4b")

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }

    try {
        when {
            regularInstalled && businessInstalled -> {
                val chooser = Intent.createChooser(sendIntent, context.getString(R.string.choose_whatsapp))
                context.startActivity(chooser)
            }
            regularInstalled -> {
                sendIntent.setPackage("com.whatsapp")
                context.startActivity(sendIntent)
            }
            businessInstalled -> {
                sendIntent.setPackage("com.whatsapp.w4b")
                context.startActivity(sendIntent)
            }
            else -> {
                Toast.makeText(context, context.getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
            }
        }
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
    }
}

fun launchWhatsAppDirect(context: Context, phone: String, message: String) {
    val url = WhatsAppUtil.getWhatsAppIntentUrl(phone, message)
    if (url != null) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun TechMechEnquirySheet(
    state: TechMechEnquiryState,
    onDismiss: () -> Unit
) {
    TechMechEnquirySheet(
        job = state.job,
        message = state.message,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechMechEnquirySheet(
    job: Job,
    message: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme

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
            Text(
                text = stringResource(R.string.ask_techmech_title, job.displayId),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.onSurface
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    launchWhatsAppShare(context, message)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_choose_contact_or_group),
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    launchWhatsAppDirect(context, TechMechContact.ENQUIRY_PHONE, message)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_message_techmech_number),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
