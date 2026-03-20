package com.billage.app.data.remote.api

import com.billage.app.ui.chat.models.UserSearchResponse
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getMyInfo(): UserMeApiResponse

    @GET("api/v1/users/contain")
    suspend fun searchUsers(
        @Query("email") email: String
    ): List<UserSearchResponse>
}

@Serializable
data class UserMeApiResponse(
    val status: Int,
    val message: String,
    val data: UserMeResponse
)

@Serializable
data class UserMeResponse(
    val userSeq: Int,
    val realName: String? = null,
    val email: String? = null,
    val phone: String? = null
)
