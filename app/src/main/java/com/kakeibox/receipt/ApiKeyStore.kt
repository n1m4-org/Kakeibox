package com.kakeibox.receipt

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Anthropic の API キーを保存する。
 * キーは Android Keystore の鍵（端末外に出せない）で AES-GCM 暗号化して保存し、
 * 端末のバックアップ・アプリのバックアップ（JSON）には含めない。
 */
class ApiKeyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _hasKey = MutableStateFlow(prefs.contains(KEY_DATA))

    /** API キーが登録されているか */
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    fun get(): String? {
        val data = prefs.getString(KEY_DATA, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun set(apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val data = cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_DATA, Base64.encodeToString(data, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
        _hasKey.value = true
    }

    fun clear() {
        prefs.edit().clear().apply()
        _hasKey.value = false
    }

    /** 画面に出す伏せ字（例: sk-ant-…a1b2） */
    fun masked(): String? = get()?.let { k -> if (k.length <= 12) "••••" else "${k.take(7)}…${k.takeLast(4)}" }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    companion object {
        /** バックアップ規則で除外するファイル名（res/xml/backup_rules.xml と合わせる） */
        const val PREFS = "kakeibox_secrets"
        private const val KEY_DATA = "anthropic_api_key"
        private const val KEY_IV = "anthropic_api_key_iv"
        private const val ALIAS = "kakeibox_api_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
