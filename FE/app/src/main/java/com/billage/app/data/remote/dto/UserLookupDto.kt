package com.billage.app.data.remote.dto

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────
// POST /api/v1/users/lookup/phone Request
// ─────────────────────────────────────────
@Serializable
data class UserLookupRequest(
    val realName: String,
    val phone: String      // 숫자만, 예: "01012345678"
)

// ─────────────────────────────────────────
// POST /api/v1/users/lookup/phone Response
// ─────────────────────────────────────────
@Serializable
data class UserLookupResponse(
    val status: Int,
    val message: String,
    val data: UserLookupData?
)

@Serializable
data class UserLookupData(
    val userSeq: Int?,
    val registered: Boolean,
    val realName: String? = null,
    val profileImageUrl: String? = null
)
