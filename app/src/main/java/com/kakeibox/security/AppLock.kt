package com.kakeibox.security

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * アプリロック（指紋・顔認証、または端末のPIN/パターン/パスワード）。
 * 起動時と、バックグラウンドに1分以上いた後に復帰したときにロックする。
 */
object AppLock {
    private const val TIMEOUT_MS = 60_000L
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    private var unlockedOnce = false
    private var backgroundAt: Long? = null

    /** ロックが必要か（純粋な判定。テスト用に時刻を渡せる） */
    fun shouldLock(enabled: Boolean, now: Long = SystemClock.elapsedRealtime()): Boolean =
        enabled && (!unlockedOnce || backgroundAt?.let { now - it >= TIMEOUT_MS } == true)

    fun onBackground(now: Long = SystemClock.elapsedRealtime()) { backgroundAt = now }

    fun markUnlocked() {
        unlockedOnce = true
        backgroundAt = null
    }

    /** テスト用 */
    internal fun reset() { unlockedOnce = false; backgroundAt = null }

    /** 端末に画面ロック（または生体認証）が設定されているか */
    fun canAuthenticate(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {},
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailure(errString.toString())
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle("指紋・顔認証、または画面ロックのPINなど")
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
        )
    }
}
