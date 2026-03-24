package com.billage.app.ui.chat.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.billage.app.BuildConfig
import com.billage.app.data.remote.api.ContractDetailResponse
import com.billage.app.data.remote.api.ContractSignatureRequest
import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ContractSignatureUtils {
    private val signatureJson = Json {
        encodeDefaults = true
        explicitNulls = true
    }

    fun buildRequest(
        context: Context,
        contractDetail: ContractDetailResponse,
        canvasDataUrl: String
    ): ContractSignatureRequest {
        val documentHash = buildDocumentHash(contractDetail)
        return ContractSignatureRequest(
            method = "BIOMETRIC",
            deviceIdHash = buildDeviceIdHash(context),
            biometricVerified = true,
            userAgent = buildUserAgent(),
            signaturePayload = buildJsonObject {
                put("canvasDataUrl", canvasDataUrl)
                put("documentHash", documentHash)
            }
        )
    }

    fun buildDocumentHash(contractDetail: ContractDetailResponse): String {
        val canonicalDocument = signatureJson.encodeToString(
            ContractDetailResponse.serializer(),
            contractDetail
        )
        return sha256(canonicalDocument)
    }

    private fun buildDeviceIdHash(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        val raw = listOf(androidId, Build.BRAND, Build.DEVICE, Build.MODEL)
            .joinToString(separator = "|")
        return sha256(raw)
    }

    private fun buildUserAgent(): String {
        return "Billage/${BuildConfig.VERSION_NAME} " +
            "(Android ${Build.VERSION.RELEASE}; ${Build.MANUFACTURER} ${Build.MODEL})"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray())
            .joinToString(separator = "") { "%02x".format(it) }
    }
}
