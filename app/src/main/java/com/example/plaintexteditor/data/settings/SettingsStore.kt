package com.example.plaintexteditor.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** 设置文件也是私有的，和“工作区”那份分开存 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

/**
 * 应用设置。
 *
 * - `showCurrentLine`：状态栏是否显示当前光标行号（默认开）
 * - `largeFileThresholdMb`：多大的文件算“大文件”（默认 5MB，下一阶段的大文件只读模式用）
 */
class SettingsStore(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    val showCurrentLine: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_SHOW_CURRENT_LINE] ?: true
    }

    val largeFileThresholdMb: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_LARGE_FILE_MB] ?: DEFAULT_LARGE_FILE_MB
    }

    suspend fun setShowCurrentLine(enabled: Boolean): Result<Unit> = io {
        context.settingsDataStore.edit { prefs -> prefs[KEY_SHOW_CURRENT_LINE] = enabled }
        Unit
    }

    suspend fun setLargeFileThresholdMb(megabytes: Int): Result<Unit> = io {
        val safe = megabytes.coerceIn(MIN_LARGE_FILE_MB, MAX_LARGE_FILE_MB)
        context.settingsDataStore.edit { prefs -> prefs[KEY_LARGE_FILE_MB] = safe }
        Unit
    }

    private suspend fun <T> io(block: suspend () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    companion object {
        const val DEFAULT_LARGE_FILE_MB = 5
        const val MIN_LARGE_FILE_MB = 1
        const val MAX_LARGE_FILE_MB = 200

        private val KEY_SHOW_CURRENT_LINE = booleanPreferencesKey("show_current_line")
        private val KEY_LARGE_FILE_MB = intPreferencesKey("large_file_threshold_mb")
    }
}
