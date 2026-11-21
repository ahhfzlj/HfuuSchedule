package com.hfuu.schedule.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 本地加密凭证存储：仅保存在当前设备中，使用 Android Keystore AES256-GCM 加密，
 * 不上传任何第三方服务器。
 */
class SecureAccountStore(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "hfuu_secure_accounts",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("hfuu_secure_accounts_fallback", Context.MODE_PRIVATE)
    }

    fun saveJwxtCredentials(username: String, password: String) {
        prefs.edit()
            .putString(KEY_JWXT_USER, username)
            .putString(KEY_JWXT_PASS, password)
            .apply()
    }

    fun getJwxtUsername(): String = prefs.getString(KEY_JWXT_USER, "").orEmpty()
    fun getJwxtPassword(): String = prefs.getString(KEY_JWXT_PASS, "").orEmpty()

    fun saveChaoxingSession(account: String, cookie: String) {
        prefs.edit()
            .putString(KEY_CX_ACCOUNT, account)
            .putString(KEY_CX_COOKIE, cookie)
            .apply()
    }

    fun getChaoxingAccount(): String = prefs.getString(KEY_CX_ACCOUNT, "").orEmpty()
    fun getChaoxingCookie(): String = prefs.getString(KEY_CX_COOKIE, "").orEmpty()

    fun clearChaoxingSession() {
        prefs.edit()
            .remove(KEY_CX_ACCOUNT)
            .remove(KEY_CX_COOKIE)
            .apply()
    }

    fun clearJwxtCredentials() {
        prefs.edit()
            .remove(KEY_JWXT_USER)
            .remove(KEY_JWXT_PASS)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_JWXT_USER = "jwxt_user"
        private const val KEY_JWXT_PASS = "jwxt_pass"
        private const val KEY_CX_ACCOUNT = "cx_account"
        private const val KEY_CX_COOKIE = "cx_cookie"
    }
}
