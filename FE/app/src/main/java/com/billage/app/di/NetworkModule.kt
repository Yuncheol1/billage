package com.billage.app.di

import android.os.Build
import com.billage.app.data.remote.api.ChatApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // 에뮬레이터/실기기 판별하여 베이스 URL 설정
        val isEmulator = Build.FINGERPRINT.contains("generic", ignoreCase = true) || 
                         Build.MODEL.contains("sdk", ignoreCase = true) || 
                         Build.MODEL.contains("Emulator", ignoreCase = true) || 
                         Build.PRODUCT.contains("sdk", ignoreCase = true) ||
                         Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
                         Build.HARDWARE.contains("ranchu", ignoreCase = true)
        
        val baseUrl = if (isEmulator) "http://10.0.2.2:8080/" else "http://localhost:8080/"
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi {
        return retrofit.create(ChatApi::class.java)
    }
}
