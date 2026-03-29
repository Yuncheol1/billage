package com.billage.app.ui.chat.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.billage.app.BuildConfig
import com.billage.app.data.remote.api.ContractDetailResponse
import com.billage.app.data.remote.api.ContractSignatureRequest
import java.security.KeyStore
import java.security.MessageDigest
import android.util.Base64
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
        canvasDataUrl: String,
        signatureBytes: ByteArray,
        publicKeyPem: String? = null
    ): ContractSignatureRequest {
        val documentHash = buildDocumentHash(contractDetail)
        val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        return ContractSignatureRequest(
            method = "BIOMETRIC",
            deviceIdHash = buildDeviceIdHash(context),
            biometricVerified = true,
            userAgent = buildUserAgent(),
            publicKeyPem = publicKeyPem ?: getStoredPublicKeyPem(),
            signaturePayload = buildJsonObject {
                put("canvasDataUrl", canvasDataUrl)
                put("documentHash", documentHash)
            },
            signatureValue = signatureBase64
        )
    }

    private fun getStoredPublicKeyPem(): String? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val certificate = keyStore.getCertificate("billage_test_key")
            val publicKey = certificate?.publicKey
            if (publicKey != null) {
                // PKCS#8 or X.509 encoding to PEM-like format
                Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        }
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
