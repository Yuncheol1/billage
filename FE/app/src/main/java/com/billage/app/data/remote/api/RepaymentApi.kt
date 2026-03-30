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
    val remainingBalanceAfter: Double? = null,
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
    val data: RepaymentActionResult? = null
)

@Serializable
data class RepaymentActionResult(
    val scheduleSeq: Int,
    val contractSeq: Int,
    val status: String,
    val contractId: String? = null,
    val transactionDate: String? = null,
    val withdrawalAccountNo: String? = null,
    val depositAccountNo: String? = null
)

interface RepaymentApi {
    @GET("/api/v1/repayment/{contractSeq}/schedules")
    suspend fun getRepaymentSchedules(
        @Path("contractSeq") contractSeq: Int
    ): RepaymentScheduleListBaseResponse

    @GET("/api/v1/repayment/schedules/upcoming")
    suspend fun getUpcomingSchedules(): UpcomingScheduleListResponse

    @POST("/api/v1/contracts/{contractSeq}/repayment-schedules/{scheduleSeq}/paid")
    suspend fun markRepaymentSchedulePaid(
        @Path("contractSeq") contractSeq: Int,
        @Path("scheduleSeq") scheduleSeq: Int
    ): RepaymentActionResponse

    @POST("/api/v1/contracts/{contractSeq}/repayment-schedules/{scheduleSeq}/transfer")
    suspend fun transferRepaymentSchedule(
        @Path("contractSeq") contractSeq: Int,
        @Path("scheduleSeq") scheduleSeq: Int
    ): RepaymentActionResponse
}
