package com.billage.app.di

import com.billage.app.data.local.AuthSessionStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authSessionStorage: AuthSessionStorage
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authSessionStorage.getAccessToken()
        
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        
        return chain.proceed(request)
    }
}
