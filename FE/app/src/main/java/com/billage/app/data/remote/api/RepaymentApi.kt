package com.billage.app.data.remote.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class RepaymentScheduleListBaseResponse(
    val status: Int,
    val message: String,
    val data: RepaymentScheduleListResponse
)

@Serializable
data class RepaymentScheduleListResponse(
    val contractSeq: Int,
    val schedules: List<RepaymentScheduleItem>
)

@Serializable
data class RepaymentScheduleItem(
    val scheduleSeq: Int,
    val installmentNo: Int,
    val dueDt: String,
    val principalDue: Double,
    val interestDue: Double,
    val totalDue: Double,
    val remainingBalanceAfter: Double,
    val status: String,
    val paidDttm: String? = null
)

@Serializable
data class UpcomingScheduleItem(
    val scheduleSeq: Int,
    val contractSeq: Int,
    val installmentNo: Int,
    val dueDt: String,
    val totalDue: Double,
    val status: String
)

@Serializable
data class UpcomingScheduleListResponse(
    val status: Int,
    val message: String,
    val data: List<UpcomingScheduleItem>
)

@Serializable
data class RepaymentActionResponse(
    val status: Int,
    val message: String,
    val data: RepaymentActionResult
)

@Serializable
data class RepaymentActionResult(
    val scheduleSeq: Int,
    val scheduleStatus: String,
    val paidDttm: String? = null,
    val contractSeq: Int,
    val contractStatus: String
)

interface RepaymentApi {
    @GET("/api/v1/repayment/{contractSeq}/schedules")
    suspend fun getRepaymentSchedules(
        @Path("contractSeq") contractSeq: Int
    ): RepaymentScheduleListBaseResponse

    @GET("/api/v1/repayment/schedules/upcoming")
    suspend fun getUpcomingSchedules(): UpcomingScheduleListResponse

    @POST("/api/v1/repayment/schedules/{scheduleSeq}/manual-pay")
    suspend fun markManualRepayment(
        @Path("scheduleSeq") scheduleSeq: Int
    ): RepaymentActionResponse
}
