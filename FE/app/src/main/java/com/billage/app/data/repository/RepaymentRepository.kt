package com.billage.app.data.repository

import com.billage.app.data.remote.api.RepaymentApi
import com.billage.app.data.remote.api.RepaymentActionResult
import com.billage.app.data.remote.api.RepaymentScheduleItem
import com.billage.app.data.remote.api.UpcomingScheduleItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepaymentRepository @Inject constructor(
    private val repaymentApi: RepaymentApi
) {
    suspend fun getRepaymentSchedules(contractSeq: Int): List<RepaymentScheduleItem> {
        return repaymentApi.getRepaymentSchedules(contractSeq).data.schedules
    }

    suspend fun getUpcomingSchedules(): List<UpcomingScheduleItem> {
        return repaymentApi.getUpcomingSchedules().data
    }

    suspend fun markRepaymentSchedulePaid(contractSeq: Int, scheduleSeq: Int): RepaymentActionResult? {
        return repaymentApi.markRepaymentSchedulePaid(contractSeq, scheduleSeq).data
    }

    suspend fun transferRepaymentSchedule(contractSeq: Int, scheduleSeq: Int): RepaymentActionResult? {
        return repaymentApi.transferRepaymentSchedule(contractSeq, scheduleSeq).data
    }
}
