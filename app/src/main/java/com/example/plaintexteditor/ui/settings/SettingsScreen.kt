package com.example.plaintexteditor.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.plaintexteditor.R
import com.example.plaintexteditor.data.settings.SettingsStore
import com.example.plaintexteditor.data.text.LineEnding
import com.example.plaintexteditor.data.text.TextEncoding
import com.example.plaintexteditor.ui.common.ActionSheetRow
import kotlinx.coroutines.launch

/** 当前打开的是哪个选择面板 */
private enum class SettingsSheet {
    THEME, FONT_SIZE, DRAFT_INTERVAL, THRESHOLD, ENCODING, LINE_ENDING
}

/**
 * 设置页。所有开关都直接读写 DataStore，改完立刻生效（主题会马上切换）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeName by settingsStore.themeModeName.collectAsState(
        initial = SettingsStore.THEME_FOLLOW_SYSTEM,
    )
    val fontSize by settingsStore.fontSizeSp.collectAsState(initial = SettingsStore.DEFAULT_FONT_SIZE_SP)
    val showCurrentLine by settingsStore.showCurrentLine.collectAsState(initial = true)
    val draftInterval by settingsStore.draftIntervalSeconds.collectAsState(
        initial = SettingsStore.DEFAULT_DRAFT_INTERVAL_SECONDS,
    )
    val thresholdMb by settingsStore.largeFileThresholdMb.collectAsState(
        initial = SettingsStore.DEFAULT_LARGE_FILE_MB,
    )
    val defaultEncoding by settingsStore.defaultEncodingName.collectAsState(initial = "UTF-8")
    val defaultLineEnding by settingsStore.defaultLineEndingName.collectAsState(initial = "LF")

    var openSheet by remember { mutableStateOf<SettingsSheet?>(null) }
    var showLicenses by remember { mutableStateOf(false) }
    val appVersion = remember(context) { appVersionName(context) }

    val themeLabel = when (themeName) {
        SettingsStore.THEME_LIGHT -> stringResource(R.string.theme_light)
        SettingsStore.THEME_DARK -> stringResource(R.string.theme_dark)
        else -> stringResource(R.string.theme_follow_system)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ---------------- 外观 ----------------
            item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_theme),
                    value = themeLabel,
                    supporting = stringResource(R.string.theme_value_hint),
                    onClick = { openSheet = SettingsSheet.THEME },
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_font_size),
                    value = stringResource(R.string.settings_value_sp, fontSize),
                    onClick = { openSheet = SettingsSheet.FONT_SIZE },
                )
            }

            // ---------------- 编辑器 ----------------
            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_editor)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_show_current_line),
                    value = null,
                    trailing = {
                        Switch(
                            checked = showCurrentLine,
                            onCheckedChange = { enabled ->
                                scope.launch { settingsStore.setShowCurrentLine(enabled) }
                            },
                        )
                    },
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_draft_interval),
                    value = stringResource(R.string.settings_value_seconds, draftInterval),
                    onClick = { openSheet = SettingsSheet.DRAFT_INTERVAL },
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_default_encoding),
                    value = defaultEncoding,
                    onClick = { openSheet = SettingsSheet.ENCODING },
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_default_line_ending),
                    value = defaultLineEnding,
                    supporting = stringResource(R.string.settings_default_hint),
                    onClick = { openSheet = SettingsSheet.LINE_ENDING },
                )
            }

            // ---------------- 文件 ----------------
            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_files)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_large_file_threshold),
                    value = stringResource(R.string.large_file_threshold_option, thresholdMb),
                    supporting = stringResource(R.string.sheet_large_file_threshold_hint),
                    onClick = { openSheet = SettingsSheet.THRESHOLD },
                )
            }

            // ---------------- 关于 ----------------
            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_about)) }
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_version),
                    value = appVersion,
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_licenses),
                    value = null,
                    onClick = { showLicenses = true },
                )
            }
        }
    }

    // ---------------- 各种选择面板 ----------------
    openSheet?.let { sheet ->
        ModalBottomSheet(onDismissRequest = { openSheet = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = sheetTitle(sheet),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                )
                HorizontalDivider()

                when (sheet) {
                    SettingsSheet.THEME -> listOf(
                        stringResource(R.string.theme_follow_system) to SettingsStore.THEME_FOLLOW_SYSTEM,
                        stringResource(R.string.theme_light) to SettingsStore.THEME_LIGHT,
                        stringResource(R.string.theme_dark) to SettingsStore.THEME_DARK,
                    ).forEach { (label, value) ->
                        ActionSheetRow(
                            text = label + if (value == themeName) currentSuffix() else "",
                            onClick = {
                                openSheet = null
                                scope.launch { settingsStore.setThemeModeName(value) }
                            },
                        )
                    }

                    SettingsSheet.FONT_SIZE -> (12..24 step 2).forEach { size ->
                        ActionSheetRow(
                            text = stringResource(R.string.settings_value_sp, size) +
                                if (size == fontSize) currentSuffix() else "",
                            onClick = {
                                openSheet = null
                                scope.launch { settingsStore.setFontSizeSp(size) }
                            },
                        )
                    }

                    SettingsSheet.DRAFT_INTERVAL -> listOf(5, 10, 30, 60).forEach { seconds ->
                        ActionSheetRow(
                            text = stringResource(R.string.settings_value_seconds, seconds) +
                                if (seconds == draftInterval) currentSuffix() else "",
                            onClick = {
                                openSheet = null
                                scope.launch { settingsStore.setDraftIntervalSeconds(seconds) }
                            },
                        )
                    }

                    SettingsSheet.THRESHOLD -> listOf(1, 2, 5, 10, 20, 50).forEach { megabytes ->
                        ActionSheetRow(
                            text = stringResource(R.string.large_file_threshold_option, megabytes) +
                                if (megabytes == thresholdMb) currentSuffix() else "",
                            onClick = {
                                openSheet = null
                                scope.launch { settingsStore.setLargeFileThresholdMb(megabytes) }
                            },
                        )
                    }

                    SettingsSheet.ENCODING -> TextEncoding.values().forEach { encoding ->
                        ActionSheetRow(
                            text = encoding.displayName +
                                if (encoding.charsetName == defaultEncoding) currentSuffix() else "",
                            onClick = {
                                openSheet = null
                                scope.launch {
                                    settingsStore.setDefaultEncodingName(encoding.charsetName)
                                }
                            },
                        )
                    }

                    SettingsSheet.LINE_ENDING -> LineEnding.values().forEach { ending ->
                        ActionSheetRow(
                            text = ending.displayName +
                                if (ending.name == defaultLineEnding) currentSuffix() else "",
                            onClick = {
                                openSheet = null
                                scope.launch { settingsStore.setDefaultLineEndingName(ending.name) }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(text = stringResource(R.string.settings_licenses)) },
            text = { Text(text = stringResource(R.string.licenses_text)) },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) {
                    Text(text = stringResource(R.string.dialog_confirm))
                }
            },
        )
    }
}

/** 当前值后面那个小标记 */
@Composable
private fun currentSuffix(): String = stringResource(R.string.label_current)

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    value: String?,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick != null) {
            Modifier.fillMaxWidth().clickable(onClick = onClick)
        } else {
            Modifier.fillMaxWidth()
        },
        headlineContent = { Text(text = title) },
        supportingContent = supporting?.let { text -> { Text(text = text) } },
        trailingContent = when {
            trailing != null -> trailing
            value != null -> {
                {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> null
        },
    )
}

@Composable
private fun sheetTitle(sheet: SettingsSheet): String = when (sheet) {
    SettingsSheet.THEME -> stringResource(R.string.settings_theme)
    SettingsSheet.FONT_SIZE -> stringResource(R.string.settings_font_size)
    SettingsSheet.DRAFT_INTERVAL -> stringResource(R.string.settings_draft_interval)
    SettingsSheet.THRESHOLD -> stringResource(R.string.settings_large_file_threshold)
    SettingsSheet.ENCODING -> stringResource(R.string.settings_default_encoding)
    SettingsSheet.LINE_ENDING -> stringResource(R.string.settings_default_line_ending)
}

/** 读应用版本号（读不到就显示 —） */
private fun appVersionName(context: Context): String = runCatching {
    val manager = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        manager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        manager.getPackageInfo(context.packageName, 0)
    }
    info.versionName ?: "—"
}.getOrDefault("—")
