package com.billage.app.data.remote.api

import com.billage.app.data.remote.dto.UserLookupRequest
import com.billage.app.data.remote.dto.UserLookupResponse
import com.billage.app.ui.chat.models.UserSearchResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserApi {

    // 차용증 생성용 — 실명 + 전화번호로 빌리지 가입 여부 조회
    // POST /api/v1/users/lookup/phone
    @POST("api/v1/users/lookup/phone")
    suspend fun lookupByPhone(
        @Body request: UserLookupRequest
    ): UserLookupResponse

    // 채팅방 생성용 — 이메일로 사용자 검색
    // GET /api/v1/users/contain?email={query}
    @GET("api/v1/users/contain")
    suspend fun searchUsersByEmail(
        @Query("email") email: String
    ): List<UserSearchResponse>
}