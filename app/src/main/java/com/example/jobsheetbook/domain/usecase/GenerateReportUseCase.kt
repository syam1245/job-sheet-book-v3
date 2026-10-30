package com.example.jobsheetbook.domain.usecase

import com.example.jobsheetbook.data.local.dao.JobDao
import com.example.jobsheetbook.data.repository.ChipLevelRepository
import com.example.jobsheetbook.util.DailyReportGenerator
import javax.inject.Inject

class GenerateReportUseCase @Inject constructor(
    private val jobDao: JobDao,
    private val chipLevelRepo: ChipLevelRepository
) {
    suspend fun generateDailyReport(): String {
        return DailyReportGenerator().generateReport(jobDao, chipLevelRepo)
    }

    suspend fun generateRangeReport(startDate: Long, endDate: Long): String {
        return DailyReportGenerator().generateRangeReport(jobDao, chipLevelRepo, startDate, endDate)
    }
}
