package com.medicontrol.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

/** Onde o app guarda a configuração do backup automático diário: se está ligado, pra qual arquivo, e quando rodou pela última vez. */
object AutoBackupPrefs {
    private const val PREFS_NAME = "auto_backup"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_URI = "uri"
    private const val KEY_LAST_SUCCESS = "last_success"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun getUri(context: Context): Uri? = prefs(context).getString(KEY_URI, null)?.let(Uri::parse)

    fun getLastSuccess(context: Context): Long? =
        prefs(context).getLong(KEY_LAST_SUCCESS, -1L).takeIf { it >= 0 }

    fun enable(context: Context, uri: Uri) {
        prefs(context).edit {
            putBoolean(KEY_ENABLED, true)
            putString(KEY_URI, uri.toString())
        }
    }

    fun disable(context: Context) {
        prefs(context).edit { putBoolean(KEY_ENABLED, false) }
    }

    fun markSuccess(context: Context, timestampMillis: Long) {
        prefs(context).edit { putLong(KEY_LAST_SUCCESS, timestampMillis) }
    }
}
