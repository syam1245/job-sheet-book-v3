package com.example.jobsheetbook.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.stringResource
import com.example.jobsheetbook.R
import com.example.jobsheetbook.util.PhoneNormalizer
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jobsheetbook.ui.theme.*
import com.example.jobsheetbook.ui.viewmodel.JobViewModel
import com.example.jobsheetbook.ui.viewmodel.JobFormUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EntryFormScreen(
    onSave: (Long) -> Unit,
    onBack: () -> Unit,
    onCaptureImages: () -> Unit,
    viewModel: JobViewModel = hiltViewModel()
) {
    val uiState by viewModel.jobFormUiState.collectAsStateWithLifecycle()
    
    val isEditing = uiState.isEditing
    val jobIdInput = uiState.jobIdInput
    val jobIdError = uiState.jobIdError
    
    val customerPhone = uiState.customerPhone
    val customerName = uiState.customerName

    val make = uiState.make
    val model = uiState.model
    val serialNumber = uiState.serialNumber
    val devicePassword = uiState.devicePassword
    val remarks = uiState.remarks
    val itemType = uiState.itemType

    val jobTypes = uiState.jobTypes
    val dueDate = uiState.dueDate
    val pendingReason = uiState.pendingReason
    val estimatedCost = uiState.estimatedCost
    val finalCost = uiState.finalCost
    val createdAtInput = uiState.createdAtInput

    val capturedImages = uiState.capturedImageUris
    val isDark = MaterialTheme.colorScheme.background == com.example.jobsheetbook.ui.theme.DeepNavy

    // Make selection state (chips)
    var selectedBrand by remember(itemType) { mutableStateOf<String?>(null) }
    var showCustomBrand by remember(itemType) { mutableStateOf(false) }

    var selectedComplaints by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customRemarks by remember { mutableStateOf("") }
    var complaintsDropdownExpanded by remember { mutableStateOf(false) }
    var problemSearchQuery by remember { mutableStateOf("") }

    val editingJobId = uiState.editingJobId
    val preselectedBrand = uiState.preselectedBrand
    val preselectedComplaints = uiState.preselectedComplaints
    val preselectedCustomRemarks = uiState.preselectedCustomRemarks

    // When editing, restore the UI state for brands, complaints, and free-text remarks.
    LaunchedEffect(editingJobId, preselectedBrand, preselectedComplaints, preselectedCustomRemarks) {
        if (isEditing) {
            selectedBrand = preselectedBrand
            showCustomBrand = preselectedBrand == "Others"
            selectedComplaints = preselectedComplaints
            customRemarks = preselectedCustomRemarks
        }
    }

    val isFormValid = viewModel.isFormValid(uiState)

    // Detect keyboard visibility to hide bottom bar
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isEditing) "Edit Job" else "New Job",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "#$jobIdInput",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalSemanticTypography.current.jobId
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Hide bottom bar when keyboard is visible to give more screen space
            if (!isKeyboardVisible) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            viewModel.saveJob { id -> onSave(id) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        enabled = isFormValid,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        Text(
                            when {
                                uiState.isQuickMode -> stringResource(R.string.action_save_as_delivered)
                                isEditing -> stringResource(R.string.action_update_job)
                                else -> stringResource(R.string.action_create_job)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            if (!isEditing) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = !uiState.isQuickMode,
                        onClick = { viewModel.setQuickMode(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.toggle_full_job))
                    }
                    SegmentedButton(
                        selected = uiState.isQuickMode,
                        onClick = { viewModel.setQuickMode(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.toggle_quick_job))
                    }
                }

                if (uiState.isQuickMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.quick_job_banner_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            FormSectionCard(
                icon = Icons.Outlined.Person,
                title = "Customer Details"
            ) {
                OutlinedTextField(
                    value = jobIdInput,
                    onValueChange = { viewModel.onJobIdChange(it) },
                    label = { Text("Job Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = jobIdError != null,
                    supportingText = { jobIdError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    colors = premiumTextFieldColors(LocalSemanticTypography.current.jobId)
                )
                Spacer(Modifier.height(12.dp))
                var phoneWasFocused by remember { mutableStateOf(false) }
                val phoneNorm = PhoneNormalizer.normalize(customerPhone)
                val isPhoneInvalidAndNonBlank = customerPhone.isNotBlank() && phoneNorm is PhoneNormalizer.Result.Invalid

                PremiumTextField(
                    value = customerPhone,
                    onValueChange = { input ->
                        val filtered = input.filter { it in '0'..'9' || it in "+ -()." }.take(20)
                        viewModel.onCustomerPhoneChange(filtered)
                    },
                    label = "Mobile Number",
                    keyboardType = KeyboardType.Phone,
                    color = LocalSemanticTypography.current.phone,
                    isError = isPhoneInvalidAndNonBlank,
                    supportingText = if (isPhoneInvalidAndNonBlank) {
                        { Text(stringResource(R.string.error_phone_invalid)) }
                    } else null,
                    onFocusChanged = { focusState ->
                        if (phoneWasFocused && !focusState.isFocused) {
                            val norm = PhoneNormalizer.normalize(customerPhone)
                            if (norm is PhoneNormalizer.Result.Valid) {
                                viewModel.onCustomerPhoneChange(PhoneNormalizer.formatForDisplay(norm.stored))
                            }
                        }
                        phoneWasFocused = focusState.isFocused
                    }
                )
                Spacer(Modifier.height(12.dp))
                PremiumTextField(
                    value = customerName,
                    onValueChange = { viewModel.onCustomerNameChange(it) },
                    label = "Customer Name",
                    color = LocalSemanticTypography.current.customerName
                )
            }

            FormSectionCard(
                icon = Icons.AutoMirrored.Outlined.Assignment,
                title = "Job Details"
            ) {
                if (isEditing) {
                    var showCreationDatePicker by remember { mutableStateOf(false) }
                    val creationDatePickerState = rememberDatePickerState(initialSelectedDateMillis = createdAtInput)
                    
                    val creationDateText = remember(createdAtInput) {
                        if (createdAtInput == null) "" else {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(createdAtInput!!))
                        }
                    }
                    
                    OutlinedTextField(
                        value = creationDateText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Creation Date") },
                        trailingIcon = { 
                            IconButton(onClick = { showCreationDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = if (isDark) DateTimeDark else DateTimeLight)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showCreationDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = premiumTextFieldColors(if (isDark) DateTimeDark else DateTimeLight)
                    )

                    if (showCreationDatePicker) {
                        DatePickerDialog(
                            onDismissRequest = { showCreationDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    creationDatePickerState.selectedDateMillis?.let { viewModel.onCreatedAtChange(it) }
                                    showCreationDatePicker = false
                                }) { Text("OK") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreationDatePicker = false }) { Text("Cancel") }
                            }
                        ) {
                            DatePicker(state = creationDatePickerState)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text("Job Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                val allJobTypes = listOf("HW Repair", "SW Install", "Networking", "Upgrade", "Remote", "On-site", "Data/Backup")
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allJobTypes.forEach { type ->
                        val isSelected = jobTypes.contains(type)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newList = if (isSelected) jobTypes - type else jobTypes + type
                                viewModel.onJobTypesChange(newList)
                            },
                            label = { Text(type) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState()
                
                val dueDateText = remember(dueDate) {
                    if (dueDate == null) "" else {
                        val status = viewModel.getDueDateStatus(dueDate)
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dueDate!!))
                        when (status) {
                            com.example.jobsheetbook.ui.viewmodel.DueDateStatus.TODAY -> "Today ($dateStr)"
                            com.example.jobsheetbook.ui.viewmodel.DueDateStatus.OVERDUE -> "Overdue ($dateStr)"
                            else -> dateStr
                        }
                    }
                }
                
                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due Date (Optional)") },
                    trailingIcon = { 
                        Row {
                            if (dueDate != null) {
                                IconButton(onClick = { viewModel.onDueDateChange(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Due Date")
                                }
                            }
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = if (isDark) DateTimeDark else DateTimeLight)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = premiumTextFieldColors(if (isDark) DateTimeDark else DateTimeLight)
                )

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.onDueDateChange(datePickerState.selectedDateMillis)
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }

            FormSectionCard(
                icon = Icons.Outlined.Devices,
                title = "Device Specifications"
            ) {
                // ── Item Type Dropdown ──
                var itemTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = itemTypeExpanded,
                    onExpandedChange = { itemTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = itemType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Item Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemTypeExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = premiumTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = itemTypeExpanded,
                        onDismissRequest = { itemTypeExpanded = false }
                    ) {
                        com.example.jobsheetbook.data.local.ITEM_TYPES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.onItemTypeChange(option)
                                    itemTypeExpanded = false
                                },
                                trailingIcon = if (option == itemType) {
                                    { Icon(Icons.Default.Close, contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Make: Cascading Chips ──
                val currentMakes = com.example.jobsheetbook.data.local.makesForItemType(itemType)

                if (currentMakes.isNotEmpty()) {
                    Text("Make", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    LazyRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentMakes) { brand ->
                            val isSelected = selectedBrand == brand
                            val brandColor = if (brand == "Others") {
                                Color(0xFF757575)
                            } else {
                                val colors = listOf(
                                    Color(0xFF0096D6), Color(0xFF007DB8), Color(0xFFE2231A),
                                    Color(0xFF00539B), Color(0xFF83B81A), Color(0xFF555555),
                                    Color(0xFF1428A0), Color(0xFFD00000), Color(0xFFF59E0B),
                                    Color(0xFF8B5CF6), Color(0xFF10B981)
                                )
                                colors[kotlin.math.abs(brand.hashCode()) % colors.size]
                            }

                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        selectedBrand = brand
                                        if (brand == "Others") {
                                            showCustomBrand = true
                                            viewModel.onMakeChange("")
                                        } else {
                                            showCustomBrand = false
                                            viewModel.onMakeChange(brand)
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                         else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(brandColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = brand.take(2).uppercase(),
                                            color = androidx.compose.ui.graphics.Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = brand,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                if (showCustomBrand || currentMakes.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    PremiumTextField(
                        value = make,
                        onValueChange = { viewModel.onMakeChange(it) },
                        label = if (currentMakes.isEmpty()) "Make / Brand" else "Custom Make"
                    )
                }

                Spacer(Modifier.height(12.dp))
                PremiumTextField(
                    value = model,
                    onValueChange = { viewModel.onModelChange(it) },
                    label = "Model"
                )
                Spacer(Modifier.height(12.dp))
                PremiumTextField(
                    value = serialNumber,
                    onValueChange = { viewModel.onSerialNumberChange(it) },
                    label = "Serial Number"
                )
                Spacer(Modifier.height(12.dp))
                PremiumTextField(
                    value = devicePassword,
                    onValueChange = { viewModel.onDevicePasswordChange(it) },
                    label = "Device Password"
                )
            }

            FormSectionCard(
                icon = Icons.Outlined.CameraAlt,
                title = "Image Documentation"
            ) {
                OutlinedButton(
                    onClick = onCaptureImages,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Capture Condition Photos",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (capturedImages.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.animateContentSize()
                    ) {
                        items(capturedImages) { uri ->
                            Card(
                                modifier = Modifier.size(100.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(uri)
                                        .size(300) // Downsample to 300px
                                        .memoryCacheKey(uri.toString() + "_thumb")
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            FormSectionCard(
                icon = Icons.Outlined.Inventory2,
                title = "Accessories"
            ) {
                val accessoriesList = listOf(
                    "Power Adapter" to "adapter",
                    "Power Cord" to "cord",
                    "Battery" to "battery",
                    "Carry Case" to "bag",
                    "USB Mouse" to "mouse"
                )
                accessoriesList.forEach { (label, key) ->
                    PremiumAccessoryCheckbox(label, key, viewModel)
                }
            }

            FormSectionCard(
                icon = Icons.AutoMirrored.Outlined.Notes,
                title = "Remarks"
            ) {
                val scope = rememberCoroutineScope()
                val remarksBringIntoViewRequester = remember { BringIntoViewRequester() }
                val pendingBringIntoViewRequester = remember { BringIntoViewRequester() }

                Text("Common Issues", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))

                // Searchable multi-select dropdown
                ExposedDropdownMenuBox(
                    expanded = complaintsDropdownExpanded,
                    onExpandedChange = { complaintsDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = problemSearchQuery,
                        onValueChange = {
                            problemSearchQuery = it
                            complaintsDropdownExpanded = true
                        },
                        placeholder = { Text("Search or select issues…", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = complaintsDropdownExpanded)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = premiumTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = complaintsDropdownExpanded,
                        onDismissRequest = {
                            complaintsDropdownExpanded = false
                            problemSearchQuery = ""
                        }
                    ) {
                        val filtered = com.example.jobsheetbook.data.local.COMMON_COMPLAINTS
                            .filter { it.contains(problemSearchQuery, ignoreCase = true) }
                        if (filtered.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No matching issues", style = MaterialTheme.typography.bodySmall) },
                                onClick = {},
                                enabled = false
                            )
                        } else {
                            filtered.forEach { complaint ->
                                val isChecked = selectedComplaints.contains(complaint)
                                DropdownMenuItem(
                                    text = { Text(complaint) },
                                    onClick = {
                                        val newSet = if (isChecked) selectedComplaints - complaint else selectedComplaints + complaint
                                        selectedComplaints = newSet
                                        viewModel.onRemarksChange(combineRemarks(newSet, customRemarks))
                                        // do NOT dismiss — allow multi-pick
                                    },
                                    leadingIcon = {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null // handled by row click
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Selected complaints shown as removable chips
                if (selectedComplaints.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedComplaints.forEach { complaint ->
                            InputChip(
                                selected = true,
                                onClick = {},
                                label = { Text(complaint, style = MaterialTheme.typography.labelMedium) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove $complaint",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                val newSet = selectedComplaints - complaint
                                                selectedComplaints = newSet
                                                viewModel.onRemarksChange(combineRemarks(newSet, customRemarks))
                                            }
                                    )
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = customRemarks,
                    onValueChange = { 
                        customRemarks = it
                        viewModel.onRemarksChange(combineRemarks(selectedComplaints, it))
                    },
                    label = { Text("Additional details") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(remarksBringIntoViewRequester)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                scope.launch { remarksBringIntoViewRequester.bringIntoView() }
                            }
                        },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Default
                    ),
                    colors = premiumTextFieldColors()
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = pendingReason,
                    onValueChange = { viewModel.onPendingReasonChange(it) },
                    label = { Text("Pending Reason (if applicable)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(pendingBringIntoViewRequester)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                scope.launch { pendingBringIntoViewRequester.bringIntoView() }
                            }
                        },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    colors = premiumTextFieldColors()
                )
                
                Spacer(Modifier.height(16.dp))
                
                PremiumTextField(
                    value = if (estimatedCost > 0) estimatedCost.toString() else "",
                    onValueChange = { viewModel.onEstimatedCostChange(it) },
                    label = "Estimated Cost (₹)",
                    keyboardType = KeyboardType.Number
                )
                
                val hasTrackerRow = uiState.hasTrackerRow
                PremiumTextField(
                    value = if (finalCost > 0) finalCost.toString() else "",
                    onValueChange = { viewModel.onFinalCostChange(it) },
                    label = "Final Cost (₹)",
                    readOnly = hasTrackerRow,
                    supportingText = if (hasTrackerRow) {
                        { Text(stringResource(R.string.final_cost_set_in_tracker)) }
                    } else null,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    color: Color = MaterialTheme.colorScheme.primary,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    onFocusChanged: ((androidx.compose.ui.focus.FocusState) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = readOnly,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    scope.launch { bringIntoViewRequester.bringIntoView() }
                }
                onFocusChanged?.invoke(focusState)
            },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        colors = premiumTextFieldColors(color)
    )
}

@Composable
private fun premiumTextFieldColors(focusedColor: Color = MaterialTheme.colorScheme.primary) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = focusedColor,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = focusedColor,
    cursorColor = focusedColor
)

@Composable
private fun PremiumAccessoryCheckbox(label: String, key: String, viewModel: JobViewModel) {
    val uiState by viewModel.jobFormUiState.collectAsStateWithLifecycle()
    val isChecked = uiState.accessories[key] == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { checked ->
                val newMap = uiState.accessories.toMutableMap()
                newMap[key] = checked
                viewModel.onAccessoriesChange(newMap)
            },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isChecked) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal
        )
    }
}

private fun combineRemarks(chips: Set<String>, freeText: String): String {
    val chipPart = chips.joinToString(", ")
    return when {
        chipPart.isNotBlank() && freeText.isNotBlank() -> "$chipPart | $freeText"
        chipPart.isNotBlank() -> chipPart
        freeText.isNotBlank() -> freeText
        else -> ""
    }
}
