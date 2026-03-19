package com.billage.app.data.remote.api

import com.billage.app.ui.chat.models.UserSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface UserApi {
    @GET("api/v1/users/search")
    suspend fun searchUsers(
        @Query("email") email: String
    ): List<UserSearchResponse>
}
