package com.example.plaintexteditor.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    /** 主题：FOLLOW_SYSTEM / LIGHT / DARK（界面负责翻译成 ThemeMode） */
    val themeModeName: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: THEME_FOLLOW_SYSTEM
    }

    /** 编辑区字号（sp） */
    val fontSizeSp: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_FONT_SIZE] ?: DEFAULT_FONT_SIZE_SP
    }

    /** 草稿自动保存间隔（秒） */
    val draftIntervalSeconds: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DRAFT_INTERVAL] ?: DEFAULT_DRAFT_INTERVAL_SECONDS
    }

    /** 默认编码（新建/空文件用；按 charsetName 存，例如 "UTF-8"） */
    val defaultEncodingName: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_ENCODING] ?: "UTF-8"
    }

    /** 默认换行符：LF / CRLF */
    val defaultLineEndingName: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_LINE_ENDING] ?: "LF"
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

    suspend fun setThemeModeName(name: String): Result<Unit> = io {
        val safe = when (name) {
            THEME_LIGHT, THEME_DARK -> name
            else -> THEME_FOLLOW_SYSTEM
        }
        context.settingsDataStore.edit { prefs -> prefs[KEY_THEME] = safe }
        Unit
    }

    suspend fun setFontSizeSp(sizeSp: Int): Result<Unit> = io {
        val safe = sizeSp.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)
        context.settingsDataStore.edit { prefs -> prefs[KEY_FONT_SIZE] = safe }
        Unit
    }

    suspend fun setDraftIntervalSeconds(seconds: Int): Result<Unit> = io {
        val safe = seconds.coerceIn(MIN_DRAFT_INTERVAL_SECONDS, MAX_DRAFT_INTERVAL_SECONDS)
        context.settingsDataStore.edit { prefs -> prefs[KEY_DRAFT_INTERVAL] = safe }
        Unit
    }

    suspend fun setDefaultEncodingName(name: String): Result<Unit> = io {
        context.settingsDataStore.edit { prefs -> prefs[KEY_DEFAULT_ENCODING] = name }
        Unit
    }

    suspend fun setDefaultLineEndingName(name: String): Result<Unit> = io {
        val safe = if (name == "CRLF") "CRLF" else "LF"
        context.settingsDataStore.edit { prefs -> prefs[KEY_DEFAULT_LINE_ENDING] = safe }
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

        const val THEME_FOLLOW_SYSTEM = "FOLLOW_SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"

        const val DEFAULT_FONT_SIZE_SP = 16
        const val MIN_FONT_SIZE_SP = 12
        const val MAX_FONT_SIZE_SP = 28

        const val DEFAULT_DRAFT_INTERVAL_SECONDS = 10
        const val MIN_DRAFT_INTERVAL_SECONDS = 2
        const val MAX_DRAFT_INTERVAL_SECONDS = 120

        private val KEY_SHOW_CURRENT_LINE = booleanPreferencesKey("show_current_line")
        private val KEY_LARGE_FILE_MB = intPreferencesKey("large_file_threshold_mb")
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE = intPreferencesKey("font_size_sp")
        private val KEY_DRAFT_INTERVAL = intPreferencesKey("draft_interval_seconds")
        private val KEY_DEFAULT_ENCODING = stringPreferencesKey("default_encoding")
        private val KEY_DEFAULT_LINE_ENDING = stringPreferencesKey("default_line_ending")
    }
}
