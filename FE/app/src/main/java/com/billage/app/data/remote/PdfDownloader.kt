package com.billage.app.data.remote

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.billage.app.BuildConfig
import com.billage.app.data.local.AuthSessionStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val authSessionStorage: AuthSessionStorage
) {
    suspend fun download(contractSeq: Int): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authSessionStorage.getAccessToken()
            val baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
            val url = "$baseUrl/api/v1/contracts/$contractSeq/document/pdf"
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val fileName = "차용증_${contractSeq}_${dateStr}.pdf"

            val request = Request.Builder()
                .url(url)
                .apply { if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token") }
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("서버 오류 (HTTP ${response.code})")

            val bytes = response.body?.bytes() ?: throw Exception("빈 응답")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(fileName, bytes)
            } else {
                @Suppress("DEPRECATION")
                saveToLegacyStorage(fileName, bytes)
            }

            fileName
        }
    }

    private fun saveWithMediaStore(fileName: String, bytes: ByteArray) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("MediaStore 저장 실패")

        resolver.openOutputStream(uri)?.use { it.write(bytes) }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    @Suppress("DEPRECATION")
    private fun saveToLegacyStorage(fileName: String, bytes: ByteArray) {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        FileOutputStream(File(dir, fileName)).use { it.write(bytes) }
    }
}
