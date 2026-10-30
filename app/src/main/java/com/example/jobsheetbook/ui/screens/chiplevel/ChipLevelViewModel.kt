package com.example.jobsheetbook.ui.screens.chiplevel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobsheetbook.data.repository.ChipLevelInfo
import com.example.jobsheetbook.data.repository.ChipLevelRepository
import com.example.jobsheetbook.data.repository.JobRepository
import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.domain.models.JobStatus
import com.example.jobsheetbook.domain.usecase.SaveChipLevelChargesUseCase
import com.example.jobsheetbook.ui.screens.chiplevel.components.TechMechEnquiryState
import com.example.jobsheetbook.ui.viewmodel.ChipLevelJobItem
import com.example.jobsheetbook.ui.viewmodel.TechMechPipelineSummary
import com.example.jobsheetbook.util.FollowUpState
import com.example.jobsheetbook.util.TechMechEnquiryMessage
import com.example.jobsheetbook.util.TechMechStatusUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

const val TRACKER_RECENT_DAYS = 90L

@HiltViewModel
class ChipLevelViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val chipLevelRepo: ChipLevelRepository,
    private val saveChipLevelChargesUseCase: SaveChipLevelChargesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: String) {
        _selectedFilter.value = filter
    }

    private fun isAtTechMech(status: JobStatus): Boolean =
        status == JobStatus.CHIP_LEVEL ||
        status == JobStatus.SENT_OUT ||
        status == JobStatus.RETURNED_AWAITING_DELIVERY

    private val _techMechEnquiryState = MutableStateFlow<TechMechEnquiryState?>(null)
    val techMechEnquiryState: StateFlow<TechMechEnquiryState?> = _techMechEnquiryState.asStateFlow()

    fun onJobCardLongClick(job: Job, labels: TechMechEnquiryMessage.Labels = TechMechEnquiryMessage.Labels()) {
        val status = JobStatus.fromString(job.rawStatus)
        if (!isAtTechMech(status)) return

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

    val techMechPipelineSummary: StateFlow<TechMechPipelineSummary> = combine(
        jobRepository.getAllJobs(),
        chipLevelRepo.chipLevelData
    ) { jobs, chipData ->
        val ninetyDaysAgo = System.currentTimeMillis() - TRACKER_RECENT_DAYS * 24 * 60 * 60 * 1000L
        val candidateJobs = jobs.filter { job ->
            if (job.isDeleted) return@filter false
            val atTechMech = isAtTechMech(job.status)
            if (atTechMech) {
                true
            } else {
                chipData.containsKey(job.jobId) && job.lastModified >= ninetyDaysAgo
            }
        }.map { job ->
            val info = chipData[job.jobId] ?: ChipLevelInfo(
                jobId = job.jobId,
                tmJobId = "",
                tmCost = 0,
                finalCost = 0,
                workDone = "",
                pickedUp = true,
                pickedUpAt = job.createdAt
            )
            ChipLevelJobItem(job, info)
        }

        val atTechMechJobs = candidateJobs.filter { isAtTechMech(it.job.status) }
        val backFromTechMechJobs = candidateJobs.filter { !isAtTechMech(it.job.status) }

        var needsFollowUp = 0
        var overdue = 0
        for (item in atTechMechJobs) {
            val since = item.info.pickedUpAt ?: item.job.createdAt
            when (TechMechStatusUtil.getFollowUpState(isAtTechMech = true, sinceMillis = since)) {
                FollowUpState.NEEDS_FOLLOW_UP -> needsFollowUp++
                FollowUpState.OVERDUE -> overdue++
                FollowUpState.NONE -> {}
            }
        }

        val oldestJob = atTechMechJobs.minByOrNull { it.info.pickedUpAt ?: it.job.createdAt }
        val oldestDays = oldestJob?.let {
            TechMechStatusUtil.daysSince(it.info.pickedUpAt ?: it.job.createdAt)
        } ?: 0

        val incompleteRecords = candidateJobs.count { it.info.tmJobId.isBlank() || it.info.tmCost == 0 }

        TechMechPipelineSummary(
            awaitingPickup = 0,
            atTechMech = atTechMechJobs.size,
            backInShop = backFromTechMechJobs.size,
            needsFollowUp = needsFollowUp,
            overdue = overdue,
            oldestPendingJobId = oldestJob?.job?.jobId,
            oldestPendingDays = oldestDays,
            incompleteRecords = incompleteRecords
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TechMechPipelineSummary(0, 0, 0, 0, 0, null, 0, 0)
    )

    val chipLevelJobsFlow: StateFlow<List<ChipLevelJobItem>> = combine(
        jobRepository.getAllJobs(),
        chipLevelRepo.chipLevelData,
        _searchQuery,
        _selectedFilter
    ) { jobs, chipData, query, filter ->
        val ninetyDaysAgo = System.currentTimeMillis() - TRACKER_RECENT_DAYS * 24 * 60 * 60 * 1000L
        val hasSearchQuery = query.isNotBlank()

        val candidateJobs = jobs.filter { job ->
            if (job.isDeleted) return@filter false
            val atTechMech = isAtTechMech(job.status)
            if (atTechMech) {
                true
            } else {
                val hasTrackerRow = chipData.containsKey(job.jobId)
                hasTrackerRow && (hasSearchQuery || job.lastModified >= ninetyDaysAgo)
            }
        }.map { job ->
            val info = chipData[job.jobId] ?: ChipLevelInfo(
                jobId = job.jobId,
                tmJobId = "",
                tmCost = 0,
                finalCost = 0,
                workDone = "",
                pickedUp = true,
                pickedUpAt = job.createdAt
            )
            ChipLevelJobItem(job, info)
        }

        val filteredItems = candidateJobs.filter { item ->
            // Search logic
            val matchesSearch = query.isBlank() || 
                item.job.displayId.contains(query, ignoreCase = true) ||
                item.job.customerName.contains(query, ignoreCase = true) ||
                item.job.customerPhone.contains(query, ignoreCase = true) ||
                item.job.model.contains(query, ignoreCase = true) ||
                item.info.tmJobId.contains(query, ignoreCase = true)
                
            if (!matchesSearch) return@filter false

            val atTechMech = isAtTechMech(item.job.status)
            val since = item.info.pickedUpAt ?: item.job.createdAt
            val followUpState = TechMechStatusUtil.getFollowUpState(
                isAtTechMech = atTechMech,
                sinceMillis = since
            )

            // Filter logic: All, At TechMech, Need follow-up, Overdue, Back from TechMech
            when (filter) {
                "At TechMech" -> atTechMech
                "Need follow-up", "Need Follow-up" -> atTechMech && followUpState == FollowUpState.NEEDS_FOLLOW_UP
                "Overdue" -> atTechMech && followUpState == FollowUpState.OVERDUE
                "Back from TechMech" -> !atTechMech
                else -> true // "All"
            }
        }

        // Sort: At TechMech jobs with the longest wait first, then Back from TechMech jobs by lastModified, newest first.
        val atTechMechList = filteredItems.filter { isAtTechMech(it.job.status) }
            .sortedBy { it.info.pickedUpAt ?: it.job.createdAt }
        val backFromTechMechList = filteredItems.filter { !isAtTechMech(it.job.status) }
            .sortedByDescending { it.job.lastModified }

        atTechMechList + backFromTechMechList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveChipLevelDetails(jobId: Long, tmJobId: String, tmCost: Int, finalCost: Int, workDone: String) {
        viewModelScope.launch {
            saveChipLevelChargesUseCase.execute(
                jobId = jobId,
                tmJobId = tmJobId,
                workDone = workDone,
                tmCost = tmCost,
                finalCost = finalCost
            )
        }
    }
}
