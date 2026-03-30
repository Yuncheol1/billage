package com.billage.app.ui.chat.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager

/**
 * 테스트용 독립 객체: 쉽게 지울 수 있도록 채팅 패키지 내부에 생성
 */
object SecurityTestBed {
    private const val ALIAS = "billage_test_key"

    // 0. 현재 기기의 스펙 (API 버전, StrongBox 지원 여부) 확인
    fun getDeviceSecuritySpec(context: Context): String {
        val apiLevel = Build.VERSION.SDK_INT
        val hasStrongBox = if (apiLevel >= Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } else false

        return "📱 기기 정보\n" +
               "- Android API 레벨: $apiLevel\n" +
               "- 최고 보안 칩(StrongBox) 지원: ${if(hasStrongBox) "✅ 지원함" else "❌ 미지원 (TEE 대체 사용)"}"
    }

    // 1. 키를 생성해보고, TEE인지 StrongBox인지 확인해서 결과를 문자로 리턴
    fun testKeyGenerationAndHwStatus(context: Context): String {
        try {
            // 생체 인증 등록 여부 먼저 검사
            val biometricManager = BiometricManager.from(context)
            val authStatus = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            if (authStatus != BiometricManager.BIOMETRIC_SUCCESS) {
                return "NEEDS_ENROLLMENT. 기기에 생체 인증(지문/얼굴)이 등록되어 있지 않습니다.\n설정 앱에서 PIN 등록 후 '지문'까지 반드시 추가해주세요."
            }

            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
            )

            // API 28 이상이면 StrongBox (하드웨어 전용칩) 우선 시도
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val spec = KeyGenParameterSpec.Builder(
                        ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                    ).setDigests(KeyProperties.DIGEST_SHA256)
                     .setUserAuthenticationRequired(true) // 생체인증 필수
                     .setIsStrongBoxBacked(true)         // StrongBox에 넣기
                     .build()
                    
                    keyPairGenerator.initialize(spec)
                    keyPairGenerator.generateKeyPair()
                    return checkHardwareStatus("StrongBox (안드로이드 최고 보안칩)")
                } catch (e: StrongBoxUnavailableException) {
                    // 기기에 StrongBox가 없으면 아래의 TEE로 넘어감
                } catch (e: Exception) {
                    // 이전 앱 강제종료의 원인! 에뮬레이터에 화면 잠금(지문/PIN)이 설정되지 않은 경우
                    return handleError(e)
                }
            }

            // StrongBox가 없거나 API 28 미만인 경우 TEE (격리된 보안 프로세서) 사용
            try {
                val spec = KeyGenParameterSpec.Builder(
                    ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                ).setDigests(KeyProperties.DIGEST_SHA256)
                 .setUserAuthenticationRequired(true) // 생체인증 필수
                 .build()
                
                keyPairGenerator.initialize(spec)
                keyPairGenerator.generateKeyPair()
                return checkHardwareStatus("TEE (TrustZone 일반 보안 영역)")
            } catch (e: Exception) {
                return handleError(e)
            }
        } catch (e: Exception) {
            return "오류: ${e.message}"
        }
    }

    private fun handleError(e: Exception): String {
        if (e is IllegalStateException || e.message?.contains("Secure lock screen") == true) {
            return "❌ 오류: 앱이 꺼졌던 원인입니다!\n에뮬레이터/기기에 화면 잠금(PIN 또는 지문)이 설정되어 있지 않으면 안드로이드가 보안 키 생성을 거부합니다.\n설정 앱에서 PIN을 먼저 등록해주세요."
        }
        return "❌ 키 생성 실패: ${e.message}"
    }

    // KeyInfo 클래스를 까서 진짜 하드웨어 기반인지 확인
    private fun checkHardwareStatus(attempted: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = keyStore.getKey(ALIAS, null) as PrivateKey
        val factory = KeyFactory.getInstance(privateKey.algorithm, "AndroidKeyStore")
        val keyInfo = factory.getKeySpec(privateKey, KeyInfo::class.java)

        val isHw = keyInfo.isInsideSecureHardware
        return "✅ 키 생성 성공! ($attempted)\n\n" +
               "▶ 실제 보안 칩에 저장되었는가?: ${if (isHw) "예 (isInsideSecureHardware = true)" else "아니오"}"
    }

    // 2. 생성된 키로 지문 팝업 띄우고 서명하기
    fun startBiometricSignature(
        activity: FragmentActivity,
        onSuccess: (Signature) -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        val authStatus = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (authStatus != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("기기에 생체 인증(지문/얼굴)이 등록되어 있지 않습니다.\n설정 앱에서 PIN 등록 후 '지문'까지 반드시 추가해주세요.")
            return
        }

        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(ALIAS)) {
                val genResult = testKeyGenerationAndHwStatus(activity)
                if (genResult.startsWith("❌") || genResult.startsWith("NEEDS_ENROLLMENT") || genResult.startsWith("오류:")) {
                    onError("보안 키 자동 생성 실패: $genResult")
                    return
                }
            }

            val privateKey = keyStore.getKey(ALIAS, null) as PrivateKey
            val signature = Signature.getInstance("SHA256withECDSA").apply { initSign(privateKey) }
            val cryptoObject = BiometricPrompt.CryptoObject(signature)

            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), 
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val sig = result.cryptoObject?.signature
                        if (sig != null) {
                            onSuccess(sig)
                        } else {
                            onError("❌ 서명 객체 획득 실패")
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onError("인증 에러: $errString")
                    }
                })

            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("차용증 전자서명")
                    .setSubtitle("기기 보안 영역의 키로 서명합니다.")
                    // 에뮬레이터에서 암호/지문 설정 안 했을 때 생기는 크래시 방지 방어코드
                    // allowedAuthenticators를 명시적으로 BIOMETRIC_STRONG으로만 지정
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("취소")
                    .build(),
                cryptoObject
            )
        } catch (e: Exception) {
            onError("❌ 서명 실패: ${e.message}")
        }
    }
}
