package com.example.jobsheetbook.ui.viewmodel
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobsheetbook.data.local.AppPreferences
import com.example.jobsheetbook.data.local.entities.CustomerEntity
import com.example.jobsheetbook.data.local.entities.JobEventEntity
import com.example.jobsheetbook.data.local.ThemeMode
import com.example.jobsheetbook.data.mappers.toDomain
import com.example.jobsheetbook.data.repository.ChipLevelInfo
import com.example.jobsheetbook.data.repository.ChipLevelRepository
import com.example.jobsheetbook.data.repository.JobRepository
import com.example.jobsheetbook.data.repository.NotificationStateRepository
import com.example.jobsheetbook.data.repository.PartsOrderRepository
import com.example.jobsheetbook.data.repository.PartsOrderState
import com.example.jobsheetbook.data.sync.SyncResult
import com.example.jobsheetbook.di.IoDispatcher
import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.domain.models.JobStatus
import com.example.jobsheetbook.domain.models.WALK_IN_NAME
import com.example.jobsheetbook.domain.policy.StatusTransitionPolicy
import com.example.jobsheetbook.domain.policy.TransitionResult
import com.example.jobsheetbook.domain.policy.WarningLevel
import com.example.jobsheetbook.domain.usecase.GenerateReportUseCase
import com.example.jobsheetbook.domain.usecase.ManageJobUseCase
import com.example.jobsheetbook.ui.screens.chiplevel.components.TechMechEnquiryState
import com.example.jobsheetbook.util.CustomerScope
import com.example.jobsheetbook.util.DataExportManager
import com.example.jobsheetbook.util.ExportFormat
import com.example.jobsheetbook.util.ExportScope
import com.example.jobsheetbook.util.PhoneNormalizer
import com.example.jobsheetbook.util.TechMechEnquiryMessage
import com.example.jobsheetbook.util.TextFormatUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext





data class DashboardUiState(
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null,
    val currentJobs: List<Job> = emptyList(),
    val filteredJobsCount: Int = 0,
    val totalJobsCount: Int = 0,
    val newestJobsCount: Int = 0,
    val oldJobsCount: Int = 0,
    val activeJobsCount: Int = 0,
    val receivedCount: Int = 0,
    val readyCount: Int = 0,
    val inProgressCount: Int = 0,
    val activeChipLevelCount: Int = 0,
    val returnCount: Int = 0,
    val deliveredCount: Int = 0,
    val urgentJobCount: Int = 0,
    val urgentJobs: List<Job> = emptyList(),
    val overdueJobs: List<Job> = emptyList()
)

data class JobFormUiState(
    val isEditing: Boolean = false,
    val editingJobId: Long? = null,
    val isQuickMode: Boolean = false,
    val originalPhoneWasBlank: Boolean = false,
    val hasTrackerRow: Boolean = false,
    val jobIdInput: String = "",
    val jobIdError: String? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val itemType: String = "Laptop",
    val make: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val devicePassword: String = "",
    val remarks: String = "",
    val preselectedBrand: String? = null,
    val preselectedComplaints: Set<String> = emptySet(),
    val preselectedCustomRemarks: String = "",
    val accessories: Map<String, Boolean> = emptyMap(),
    val jobTypes: List<String> = emptyList(),
    val dueDate: Long? = null,
    val pendingReason: String = "",
    val estimatedCost: Int = 0,
    val finalCost: Int = 0,
    val createdAtInput: Long? = null,
    val capturedImageUris: List<Uri> = emptyList()
)

data class JobPreviewUiState(
    val job: Job? = null,
    val events: List<JobEventEntity> = emptyList(),
    val partsState: PartsOrderState? = null
)

data class ChipLevelJobItem(
    val job: Job,
    val info: ChipLevelInfo
)

data class TechMechPipelineSummary(
    val awaitingPickup: Int,
    val atTechMech: Int,
    val backInShop: Int,
    val needsFollowUp: Int,
    val overdue: Int,
    val oldestPendingJobId: Long?,
    val oldestPendingDays: Int,
    val incompleteRecords: Int
)

enum class DueDateStatus { NONE, NORMAL, URGENT, TODAY, OVERDUE }


@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class JobViewModel @Inject constructor(
    private val repository: JobRepository,
    private val chipLevelRepo: ChipLevelRepository,
    private val appPreferences: AppPreferences,
    private val notificationStateRepo: NotificationStateRepository,
    private val partsOrderRepo: PartsOrderRepository,
    private val dataExportManager: DataExportManager,
    private val manageJobUseCase: ManageJobUseCase,
    private val generateReportUseCase: GenerateReportUseCase,
    private val transitionPolicy: StatusTransitionPolicy,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeMode

    fun setThemeMode(mode: ThemeMode) {
        appPreferences.setThemeMode(mode)
    }

    private val _dashboardUiState = MutableStateFlow(DashboardUiState())
    val dashboardUiState = _dashboardUiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatusFilter = MutableStateFlow<String?>(null)
    private val _refreshTrigger = MutableStateFlow(0)
    private val _jobsLimit = MutableStateFlow(20)

    val jobsLimit: StateFlow<Int> = _jobsLimit.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _searchQuery.debounce(300),
                _selectedStatusFilter,
                _jobsLimit,
                _refreshTrigger
            ) { query, status, limit, _ ->
                val searchKey = when (val res = PhoneNormalizer.normalize(query)) {
                    is PhoneNormalizer.Result.Valid -> res.stored
                    else -> query
                }
                repository.getJobsWithLimit(query = searchKey, status = status, limit = limit)
            }.flatMapLatest { it }.collect { jobs ->
                _dashboardUiState.update { it.copy(currentJobs = jobs) }
            }
        }

        viewModelScope.launch {
            combine(
                _searchQuery.debounce(300),
                _selectedStatusFilter,
                _refreshTrigger
            ) { query, status, _ ->
                val searchKey = when (val res = PhoneNormalizer.normalize(query)) {
                    is PhoneNormalizer.Result.Valid -> res.stored
                    else -> query
                }
                repository.getFilteredJobsCount(query = searchKey, status = status)
            }.flatMapLatest { it }.collect { count ->
                _dashboardUiState.update { it.copy(filteredJobsCount = count) }
            }
        }

        val statuses = listOf("Received", "Ready", "Work in-progress", "Chip-Level", "Return", "Delivered")
        statuses.forEach { status ->
            viewModelScope.launch {
                repository.getStatusCount(status).collect { count ->
                    _dashboardUiState.update { state ->
                        when (status) {
                            "Received" -> state.copy(receivedCount = count)
                            "Ready" -> state.copy(readyCount = count)
                            "Work in-progress" -> state.copy(inProgressCount = count)
                            "Chip-Level" -> state.copy(activeChipLevelCount = count)
                            "Return" -> state.copy(returnCount = count)
                            "Delivered" -> state.copy(deliveredCount = count)
                            else -> state
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.getTotalJobsCount().collect { count ->
                _dashboardUiState.update { it.copy(totalJobsCount = count, activeJobsCount = count) }
            }
        }
        viewModelScope.launch {
            repository.getNewestJobsCount().collect { count ->
                _dashboardUiState.update { it.copy(newestJobsCount = count) }
            }
        }
        viewModelScope.launch {
            repository.getOldJobsCount().collect { count ->
                _dashboardUiState.update { it.copy(oldJobsCount = count) }
            }
        }
        viewModelScope.launch {
            repository.getUrgentJobCount().collect { count ->
                _dashboardUiState.update { it.copy(urgentJobCount = count) }
            }
        }
        viewModelScope.launch {
            repository.getUrgentJobs().collect { jobs ->
                _dashboardUiState.update { it.copy(urgentJobs = jobs) }
            }
        }
        viewModelScope.launch {
            repository.getOverdueJobs().collect { jobs ->
                _dashboardUiState.update { it.copy(overdueJobs = jobs) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _jobsLimit.value = 20
        _dashboardUiState.update { it.copy(searchQuery = query) }
    }

    fun onStatusFilterChange(status: String?) {
        _selectedStatusFilter.value = status
        _jobsLimit.value = 20
        _dashboardUiState.update { it.copy(selectedStatusFilter = status) }
    }

    private val _techMechEnquiryState = MutableStateFlow<TechMechEnquiryState?>(null)
    val techMechEnquiryState: StateFlow<TechMechEnquiryState?> = _techMechEnquiryState.asStateFlow()

    fun onJobCardLongClick(job: Job, labels: TechMechEnquiryMessage.Labels = TechMechEnquiryMessage.Labels()) {
        val status = JobStatus.fromString(job.rawStatus)
        val isChipLevel = status == JobStatus.CHIP_LEVEL ||
                status == JobStatus.SENT_OUT ||
                status == JobStatus.RETURNED_AWAITING_DELIVERY
        if (!isChipLevel) return

        viewModelScope.launch {
            val tmJobId = chipLevelRepo.getChipLevelInfo(job.jobId)?.tmJobId ?: ""
            val message = TechMechEnquiryMessage.build(
                jobNumber = job.displayId,
                customerName = job.customerName,
                make = job.make,
                model = job.model,
                remarks = job.remarks,
                tmJobId = tmJobId,
                labels = labels
            )
            _techMechEnquiryState.value = TechMechEnquiryState(job, message)
        }
    }

    fun dismissTechMechEnquiry() {
        _techMechEnquiryState.value = null
    }

    fun loadMoreJobs() {
        _jobsLimit.value += 20
    }

    fun resetJobsLimit() {
        _jobsLimit.value = 20
    }

    fun calculateTechMechDeadline(customerDueDate: Long): Long {
        return customerDueDate - (2 * 24 * 60 * 60 * 1000L) // 2 days before
    }

    fun getDueDateStatus(dueDate: Long?): DueDateStatus {
        if (dueDate == null) return DueDateStatus.NONE
        
        // Normalize times to midnight to ignore time of day for "today" calculation
        val calDue = java.util.Calendar.getInstance().apply { 
            timeInMillis = dueDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val calToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        val daysUntilDue = (calDue.timeInMillis - calToday.timeInMillis) / (24 * 60 * 60 * 1000L)
        
        return when {
            daysUntilDue < 0 -> DueDateStatus.OVERDUE
            daysUntilDue == 0L -> DueDateStatus.TODAY
            daysUntilDue in 1..2 -> DueDateStatus.URGENT
            else -> DueDateStatus.NORMAL
        }
    }


    val lastSyncTime: StateFlow<Long?> = repository.lastSyncTime

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult.asStateFlow()

    fun triggerManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.performManualSync()
            _syncResult.value = result
            _isSyncing.value = false
            _refreshTrigger.update { it + 1 }
        }
    }
    private val _jobFormUiState = MutableStateFlow(JobFormUiState())
    val jobFormUiState = _jobFormUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val nextId = repository.getNextJobId()
            if (!_jobFormUiState.value.isEditing) {
                _jobFormUiState.update { it.copy(jobIdInput = String.format("D-%05d", nextId)) }
            }
        }

        // One-time startup fix: recover jobs wrongly archived by the instant-archive bug
        // and migrate any leftover vendor statuses back to "Chip-Level".
        if (!notificationStateRepo.hasCompletedMigration(2)) {
            viewModelScope.launch {
                try {
                    val legacyCount = repository.countLegacyVendorStatusJobs()
                    android.util.Log.d("VENDOR_CLEANUP", "Legacy vendor status count: $legacyCount")

                    // Use the actual delivery/return event date where available.
                    val cutoff = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
                    repository.unarchiveRecentDelivered(cutoff)

                    val jobs = repository.getAllJobs().first()
                    jobs.filter { it.rawStatus.equals("Sent Out", ignoreCase = true) || it.rawStatus.equals("Returned - Awaiting Delivery", ignoreCase = true) }
                        .forEach { job ->
                            manageJobUseCase.updateJob(job.copy(status = JobStatus.CHIP_LEVEL, rawStatus = "Chip-Level"))
                        }
                    notificationStateRepo.markMigrationComplete(2)
                } catch (_: Exception) { }
            }
        }


        _jobFormUiState.map { it.customerPhone }
            .distinctUntilChanged()
            .debounce(500)
            .onEach { phone ->
                val norm = PhoneNormalizer.normalize(phone)
                if (norm is PhoneNormalizer.Result.Valid) {
                    val customer = repository.getCustomerByPhone(norm.stored)
                    customer?.let {
                        _jobFormUiState.update { state -> state.copy(customerName = it.name) }
                    }
                }
            }.launchIn(viewModelScope)
            
        _jobFormUiState.map { it.jobIdInput }
            .distinctUntilChanged()
            .debounce(500)
            .onEach { idStr ->
                if (!_jobFormUiState.value.isEditing && idStr.isNotBlank()) {
                    val numericStr = idStr.trimStart { !it.isDigit() }
                    val id = numericStr.toLongOrNull()
                    if (id != null) {
                        val existingJob = repository.getJobById(id)
                        if (existingJob != null) {
                            _jobFormUiState.update { it.copy(jobIdError = "Job ID already exists") }
                        } else {
                            _jobFormUiState.update { it.copy(jobIdError = null) }
                        }
                    } else {
                        _jobFormUiState.update { it.copy(jobIdError = null) }
                    }
                } else {
                    _jobFormUiState.update { it.copy(jobIdError = null) }
                }
            }.launchIn(viewModelScope)
    }

    
    fun setQuickMode(enabled: Boolean) {
        _jobFormUiState.update { it.copy(isQuickMode = enabled) }
    }

    fun isFormValid(uiState: JobFormUiState = _jobFormUiState.value): Boolean {
        val phoneResult = PhoneNormalizer.normalize(uiState.customerPhone)
        val isOptionalContact = uiState.isQuickMode || (uiState.isEditing && uiState.originalPhoneWasBlank)
        val isContactValid = if (isOptionalContact) {
            phoneResult is PhoneNormalizer.Result.Blank || phoneResult is PhoneNormalizer.Result.Valid
        } else {
            phoneResult is PhoneNormalizer.Result.Valid && uiState.customerName.trim().isNotBlank()
        }
        return isContactValid && uiState.jobIdInput.isNotBlank() && uiState.jobIdError == null
    }

    fun onJobIdChange(id: String) {
        // Allow alphanumeric (digital ID format) or numeric
        _jobFormUiState.update { it.copy(jobIdInput = id) }
    }

    fun onCustomerPhoneChange(phone: String) {
        _jobFormUiState.update { it.copy(customerPhone = phone) }
    }

    fun onCustomerNameChange(name: String) {
        _jobFormUiState.update { it.copy(customerName = TextFormatUtil.toTitleCase(name)) }
    }

    fun onMakeChange(text: String) {
        _jobFormUiState.update { it.copy(make = TextFormatUtil.toTitleCase(text)) }
    }

    /**
     * Called whenever the user selects a new Item Type from the dropdown.
     * Resets the Make field so a stale brand from a different device type
     * is never accidentally saved.
     */
    fun onItemTypeChange(type: String) {
        _jobFormUiState.update { 
            it.copy(
                itemType = type,
                make = "",
                preselectedBrand = null
            )
        }
    }

    fun onModelChange(text: String) {
        _jobFormUiState.update { it.copy(model = TextFormatUtil.toTitleCase(text)) }
    }

    fun onSerialNumberChange(text: String) {
        _jobFormUiState.update { it.copy(serialNumber = text.uppercase(Locale.getDefault())) }
    }

    fun onDevicePasswordChange(text: String) {
        _jobFormUiState.update { it.copy(devicePassword = text) }
    }

    fun onRemarksChange(text: String) {
        _jobFormUiState.update { it.copy(remarks = TextFormatUtil.toSentenceCase(text)) }
    }

    fun onJobTypesChange(types: List<String>) {
        _jobFormUiState.update { it.copy(jobTypes = types) }
    }

    fun onDueDateChange(date: Long?) {
        _jobFormUiState.update { it.copy(dueDate = date) }
    }

    fun onCreatedAtChange(date: Long) {
        _jobFormUiState.update { it.copy(createdAtInput = date) }
    }

    fun onPendingReasonChange(reason: String) {
        _jobFormUiState.update { it.copy(pendingReason = reason) }
    }

    fun onEstimatedCostChange(cost: String) {
        _jobFormUiState.update { it.copy(estimatedCost = cost.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0) }
    }

    fun onFinalCostChange(cost: String) {
        _jobFormUiState.update { it.copy(finalCost = cost.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0) }
    }

    fun addCapturedImage(uri: Uri) {
        _jobFormUiState.update { it.copy(capturedImageUris = it.capturedImageUris + uri) }
    }

    fun onAccessoriesChange(accessories: Map<String, Boolean>) {
        _jobFormUiState.update { it.copy(accessories = accessories) }
    }

    fun loadJobForEditing(jobId: Long) {
        viewModelScope.launch {
            repository.getJobById(jobId)?.let { job ->
                val currentMakes = com.example.jobsheetbook.data.local.makesForItemType(job.itemType)
                val knownBrand = currentMakes.firstOrNull { it.equals(job.make, ignoreCase = true) && it != "Others" }
                val preBrand = knownBrand ?: if (job.make.isNotBlank()) "Others" else null

                val rawRemarks = job.remarks
                val pipeIndex = rawRemarks.indexOf(" | ")
                val complaints: Set<String>
                val customRem: String
                if (pipeIndex != -1) {
                    val chipPart = rawRemarks.substring(0, pipeIndex)
                    val freePart = rawRemarks.substring(pipeIndex + 3)
                    complaints = chipPart.split(",").map { it.trim() }
                        .filter { it in com.example.jobsheetbook.data.local.COMMON_COMPLAINTS }.toSet()
                    customRem = freePart
                } else {
                    val segments = rawRemarks.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    complaints = segments.filter { it in com.example.jobsheetbook.data.local.COMMON_COMPLAINTS }.toSet()
                    customRem = segments.filter { it !in com.example.jobsheetbook.data.local.COMMON_COMPLAINTS }.joinToString(", ")
                }

                val originalPhoneWasBlank = job.customerPhone.isBlank()
                val displayCustomerName = if (originalPhoneWasBlank && job.customerName == WALK_IN_NAME) "" else job.customerName
                val hasTrackerRow = chipLevelRepo.getChipLevelInfo(jobId) != null

                _jobFormUiState.update { state ->
                    state.copy(
                        isEditing = true,
                        editingJobId = jobId,
                        isQuickMode = false,
                        originalPhoneWasBlank = originalPhoneWasBlank,
                        hasTrackerRow = hasTrackerRow,
                        jobIdInput = job.displayId,
                        customerPhone = PhoneNormalizer.formatForDisplay(job.customerPhone),
                        customerName = displayCustomerName,
                        itemType = job.itemType,
                        make = job.make,
                        model = job.model,
                        serialNumber = job.serialNumber,
                        devicePassword = job.devicePassword,
                        remarks = job.remarks,
                        preselectedBrand = preBrand,
                        preselectedComplaints = complaints,
                        preselectedCustomRemarks = customRem,
                        accessories = job.accessories,
                        capturedImageUris = job.imagePaths.map { Uri.parse(it) },
                        jobTypes = job.jobTypes,
                        dueDate = job.dueDate,
                        pendingReason = job.pendingReason,
                        estimatedCost = job.estimatedCost,
                        finalCost = job.finalCost,
                        createdAtInput = job.createdAt
                    )
                }
            }
        }
    }

    fun deleteJob(jobId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            manageJobUseCase.deleteJob(jobId)
            _refreshTrigger.update { it + 1 }
            onComplete()
        }
    }

    fun getDeletedJobs(query: String): Flow<List<Job>> = repository.getDeletedJobs(query)

    fun restoreDeletedJob(jobId: Long, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val restored = repository.restoreDeletedJob(jobId)
            if (restored) {
                chipLevelRepo.getTombstones().firstOrNull { it.jobId == jobId }?.let { tombstone ->
                    val restoredInfo = tombstone.copy(isDeleted = false)
                    chipLevelRepo.saveChipLevelInfo(restoredInfo)
                    repository.uploadChipLevelInfo(restoredInfo)
                }
                _refreshTrigger.update { it + 1 }
            }
            onComplete(restored)
        }
    }

    suspend fun getJobById(jobId: Long): Job? {
        return repository.getJobById(jobId)
    }

    fun getJobByIdFlow(jobId: Long): Flow<Job?> {
        return repository.getJobByIdFlow(jobId)
    }

    fun getJobEvents(jobId: Long): Flow<List<JobEventEntity>> {
        return repository.getEventsForJob(jobId)
    }

    fun getJobPreviewUiState(jobId: Long, partsTrigger: Int): Flow<JobPreviewUiState> {
        return combine(
            getJobByIdFlow(jobId),
            getJobEvents(jobId)
        ) { job, events ->
            JobPreviewUiState(
                job = job,
                events = events,
                partsState = getPartsOrderState(jobId)
            )
        }
    }

    private val _pendingStatusTransition = MutableStateFlow<PendingStatusTransition?>(null)
    val pendingStatusTransition = _pendingStatusTransition.asStateFlow()

    private val _uiEvents = kotlinx.coroutines.channels.Channel<JobUiEvent>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    fun updateJobStatus(job: Job, newStatus: String, note: String = "") {
        val targetStatus = JobStatus.fromString(newStatus)
        if (targetStatus == job.status) {
            if (targetStatus == JobStatus.CHIP_LEVEL) {
                viewModelScope.launch(dispatcher) {
                    _uiEvents.send(JobUiEvent.OpenChipLevelDialog(job.jobId))
                }
            }
            return
        }
        viewModelScope.launch(dispatcher) {
            val chipInfo = chipLevelRepo.getChipLevelInfo(job.jobId)
            val result = transitionPolicy.evaluate(
                job = job,
                newStatus = targetStatus,
                trackerFinalCost = chipInfo?.finalCost
            )

            when (result) {
                is TransitionResult.Allowed -> {
                    executeJobStatusUpdate(job, newStatus, note, false)
                }
                is TransitionResult.Warn -> {
                    _pendingStatusTransition.value = PendingStatusTransition(
                        jobId = job.jobId,
                        from = job.status,
                        to = targetStatus,
                        level = result.level,
                        reasons = result.reasons,
                        note = note
                    )
                }
            }
        }
    }

    fun confirmStatusTransition() {
        val pending = _pendingStatusTransition.getAndUpdate { null } ?: return
        viewModelScope.launch(dispatcher) {
            val currentJob = repository.getJobById(pending.jobId)
            if (currentJob == null) {
                return@launch
            }
            
            if (currentJob.status != pending.from) {
                // The status changed underneath us via sync, discard and re-evaluate
                updateJobStatus(currentJob, pending.to.value, pending.note)
                return@launch
            }
            
            executeJobStatusUpdate(
                job = currentJob,
                newStatus = pending.to.value,
                note = pending.note,
                isOverride = pending.level == WarningLevel.STRONG
            )
        }
    }

    fun cancelStatusTransition() {
        _pendingStatusTransition.value = null
    }

    private suspend fun executeJobStatusUpdate(job: Job, newStatus: String, note: String, isOverride: Boolean) {
        val statusEnum = JobStatus.fromString(newStatus)
        val isTerminal = statusEnum == JobStatus.DELIVERED || statusEnum == JobStatus.RETURN
        val updatedJob = job.copy(
            status = JobStatus.fromString(newStatus),
            rawStatus = newStatus,
            isArchived = if (isTerminal) job.isArchived else false
        )
        manageJobUseCase.updateJob(updatedJob)
        notificationStateRepo.resetState(job.jobId)
        
        if (newStatus.equals("Delivered", ignoreCase = true)) {
            // Payment notifications were removed
        }
        if (newStatus.equals("Chip-Level", ignoreCase = true)) {
            val existing = chipLevelRepo.getChipLevelInfo(job.jobId)
            val now = System.currentTimeMillis()
            val info = if (existing == null) {
                com.example.jobsheetbook.data.repository.ChipLevelInfo(
                    jobId = job.jobId,
                    tmJobId = "",
                    tmCost = 0,
                    finalCost = 0,
                    workDone = "",
                    tmStatus = "Not Marked",
                    pickedUp = true,
                    pickedUpAt = now
                )
            } else {
                existing.copy(
                    pickedUp = true,
                    pickedUpAt = now
                )
            }
            chipLevelRepo.saveChipLevelInfo(info)
            repository.uploadChipLevelInfo(info)
            _uiEvents.send(JobUiEvent.OpenChipLevelDialog(job.jobId))
        }

        _refreshTrigger.update { it + 1 }
        
        val baseMessage = when (newStatus) {
            "Received" -> "Received (Reception date & time)"
            "Work in-progress" -> "Work in-progress"
            "Chip-Level" -> "Chip-Level (Status change date & time)"
            "Ready" -> "Ready"
            "Pending" -> "Pending"
            "Delivered" -> "Delivered (Final handover date & time)"
            "Return" -> "Return"
            else -> "Status changed to '$newStatus'"
        }
        
        val eventDescriptionBase = if (note.isBlank()) {
            baseMessage
        } else {
            "$baseMessage. Note: $note"
        }
        
        val finalDescription = if (isOverride) {
            POLICY_OVERRIDE_PREFIX + eventDescriptionBase
        } else {
            eventDescriptionBase
        }
        
        repository.insertEvent(
            com.example.jobsheetbook.data.local.entities.JobEventEntity(
                jobId = job.jobId,
                eventType = "STATUS_CHANGE",
                description = finalDescription
            )
        )
    }

    fun addManualNote(jobId: Long, note: String) {
        viewModelScope.launch {
            repository.insertEvent(
                JobEventEntity(
                    jobId = jobId,
                    eventType = "NOTE",
                    description = note
                )
            )
        }
    }

    fun saveJob(onComplete: (Long) -> Unit) {
        val uiState = _jobFormUiState.value
        if (!isFormValid(uiState)) return
        val input = uiState.jobIdInput.trim()
        
        viewModelScope.launch {
            val parsedId = input.trimStart { !it.isDigit() }.toLongOrNull()
            
            var finalJobId: Long = 0L
            
            if (uiState.isEditing) {
                finalJobId = uiState.editingJobId!!
                if (parsedId != null && parsedId != finalJobId) {
                    finalJobId = parsedId
                }
            } else {
                finalJobId = parsedId ?: repository.getNextJobId()
            }

            val existingTarget = repository.getJobByIdIncludingDeleted(finalJobId)
            val isEditingSameId = uiState.isEditing && finalJobId == uiState.editingJobId
            if (existingTarget != null && !isEditingSameId) {
                _jobFormUiState.update { it.copy(jobIdError = "Job ID already exists") }
                return@launch
            }
            _jobFormUiState.update { it.copy(jobIdError = null) }
            
            val normalizedPhone = when (val res = PhoneNormalizer.normalize(uiState.customerPhone)) {
                is PhoneNormalizer.Result.Valid -> res.stored
                else -> ""
            }

            if (normalizedPhone.isNotBlank()) {
                val customer = CustomerEntity(
                    phoneNumber = normalizedPhone,
                    name = uiState.customerName
                )
                repository.saveCustomer(customer)
            }

            val existingJob = if (uiState.isEditing) repository.getJobById(uiState.editingJobId!!) else null
            
            val isQuickSave = uiState.isQuickMode && !uiState.isEditing
            val rawStatusVal = when {
                isQuickSave -> "Delivered"
                uiState.isEditing -> existingJob?.rawStatus ?: "Received"
                else -> "Received"
            }
            val statusEnum = if (isQuickSave) JobStatus.DELIVERED else JobStatus.fromString(rawStatusVal)

            val job = Job(
                jobId = finalJobId,
                customerPhone = normalizedPhone,
                customerName = uiState.customerName,
                createdAt = if (uiState.isEditing) (uiState.createdAtInput ?: existingJob?.createdAt ?: System.currentTimeMillis()) else System.currentTimeMillis(),
                itemType = uiState.itemType,
                make = uiState.make,
                model = uiState.model,
                serialNumber = uiState.serialNumber,
                devicePassword = uiState.devicePassword,
                remarks = uiState.remarks,
                imagePaths = uiState.capturedImageUris.map { it.toString() },
                accessories = uiState.accessories,
                status = statusEnum,
                rawStatus = rawStatusVal,
                jobTypes = uiState.jobTypes,
                dueDate = uiState.dueDate,
                pendingReason = uiState.pendingReason,
                estimatedCost = uiState.estimatedCost,
                finalCost = uiState.finalCost,
                displayJobId = if (input.contains("-")) input else null,
                isArchived = if (rawStatusVal in listOf("Delivered", "Return")) existingJob?.isArchived ?: false else false,
                lastModified = existingJob?.lastModified ?: System.currentTimeMillis(),
                isDeleted = existingJob?.isDeleted ?: false
            )
            
            if (uiState.isEditing) {
                if (uiState.editingJobId != finalJobId) {
                    manageJobUseCase.deleteJob(uiState.editingJobId!!)
                    manageJobUseCase.saveJob(job)
                } else {
                    manageJobUseCase.updateJob(job)
                }
                repository.insertEvent(JobEventEntity(jobId = job.jobId, eventType = "NOTE", description = "Job details updated"))
            } else if (isQuickSave) {
                manageJobUseCase.saveJob(job)
                repository.insertEvent(
                    JobEventEntity(
                        jobId = job.jobId,
                        eventType = "STATUS_CHANGE",
                        description = "Job created (Quick job)"
                    )
                )
                repository.insertEvent(
                    JobEventEntity(
                        jobId = job.jobId,
                        eventType = "STATUS_CHANGE",
                        description = "Delivered (Final handover date & time)"
                    )
                )
            } else {
                manageJobUseCase.saveJob(job)
                repository.insertEvent(JobEventEntity(jobId = job.jobId, eventType = "STATUS_CHANGE", description = "Job created with status 'Received'"))
            }
            _refreshTrigger.update { it + 1 }
            onComplete(job.jobId)
            resetForm()
        }
    }

    fun resetForm() {
        viewModelScope.launch {
            val nextId = repository.getNextJobId()
            _jobFormUiState.update { JobFormUiState(jobIdInput = String.format("D-%05d", nextId), isQuickMode = false, originalPhoneWasBlank = false) }
        }
    }

    fun generateAndCopyDailyReport(context: android.content.Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            val report = generateReportUseCase.generateDailyReport()
            val clipboard = context.getSystemService(
                android.content.Context.CLIPBOARD_SERVICE
            ) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Daily Report", report)
            clipboard.setPrimaryClip(clip)
            onComplete()
        }
    }

    fun generateAndCopyRangeReport(
        context: android.content.Context,
        startDate: Long,
        endDate: Long,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val report = generateReportUseCase.generateRangeReport(startDate, endDate)
            val clipboard = context.getSystemService(
                android.content.Context.CLIPBOARD_SERVICE
            ) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Delivery Report", report)
            clipboard.setPrimaryClip(clip)
            onComplete()
        }
    }

    fun getPartsOrderState(jobId: Long): PartsOrderState? {
        return partsOrderRepo.getState(jobId)
    }

    fun markPartsOrdered(jobId: Long) {
        partsOrderRepo.markOrdered(jobId)
    }

    fun markPartsArrived(jobId: Long) {
        partsOrderRepo.markArrived(jobId)
    }

    fun confirmPartsOrder(jobId: Long) {
        partsOrderRepo.confirmOrder(jobId)
    }

    fun clearPartsState(jobId: Long) {
        partsOrderRepo.clearState(jobId)
    }

    fun getPartsStateFlow(jobId: Long): Flow<PartsOrderState?> {
        return flow {
            emit(partsOrderRepo.getState(jobId))
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun onCleared() {
        super.onCleared()
        chipLevelRepo.unregisterListener()
    }

    fun exportDatabase(scope: String, format: String, onComplete: (java.io.File) -> Unit) {
        viewModelScope.launch {
            val exportScope = if (scope == "Active Only") ExportScope.ACTIVE_ONLY else ExportScope.FULL_HISTORY
            val exportFormat = if (format == "JSON") ExportFormat.JSON else ExportFormat.CSV
            val file = dataExportManager.exportDatabase(exportScope, exportFormat)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(file)
            }
        }
    }

    fun exportCustomerList(scope: String, format: String, onComplete: (java.io.File) -> Unit) {
        viewModelScope.launch {
            val customerScope = if (scope == "Active Customers") CustomerScope.ACTIVE_ONLY else CustomerScope.ALL
            val exportFormat = if (format == "JSON") ExportFormat.JSON else ExportFormat.CSV
            val file = dataExportManager.exportCustomerList(customerScope, exportFormat)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(file)
            }
        }
    }

    companion object {
        const val POLICY_OVERRIDE_PREFIX = "[Policy Override] "
    }
}

data class PendingStatusTransition(
    val jobId: Long,
    val from: com.example.jobsheetbook.domain.models.JobStatus,
    val to: com.example.jobsheetbook.domain.models.JobStatus,
    val level: com.example.jobsheetbook.domain.policy.WarningLevel,
    val reasons: List<com.example.jobsheetbook.domain.policy.WarningReason>,
    val note: String
)

sealed interface JobUiEvent {
    data class OpenChipLevelDialog(val jobId: Long) : JobUiEvent
}
