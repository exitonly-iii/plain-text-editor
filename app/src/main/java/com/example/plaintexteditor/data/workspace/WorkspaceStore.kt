package com.example.plaintexteditor.data.workspace

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.plaintexteditor.data.model.WorkspaceItem
import com.example.plaintexteditor.data.model.WorkspaceType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * DataStore 的单例：整个应用共用这一个文件（workspace.preferences_pb），
 * 它同样放在应用私有目录里，不需要任何权限。
 */
private val Context.workspaceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "workspace",
)

/**
 * 工作区存储：负责“工作区列表”的保存和读取。
 *
 * 它只保存**路径 + 类型**，不复制、不移动任何文件：
 * 工作区只是“记住我关心哪个目录”，文件本身始终只有一份。
 *
 * 数据保存位置：应用私有目录里的 DataStore 文件（键值对存储）。
 *
 * @param context 传 Application 的 context（不要传 Activity，避免内存泄漏）
 */
class WorkspaceStore(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /**
     * 工作区列表。它是 Flow：DataStore 里的内容一变，这里就会自动收到新列表，
     * 下一阶段的界面直接 collect 它就能自动刷新。
     */
    val workspaces: Flow<List<WorkspaceItem>> = context.workspaceDataStore.data.map { prefs ->
        decode(prefs[KEY].orEmpty())
    }

    /** 添加一个工作区（已经存在同路径的就不会重复添加）。 */
    suspend fun add(path: String, type: WorkspaceType): Result<Unit> = io {
        val item = WorkspaceItem.fromPath(normalizePath(path), type)
        context.workspaceDataStore.edit { prefs ->
            val current = decode(prefs[KEY].orEmpty())
            if (current.none { it.path == item.path }) {
                prefs[KEY] = encode(current + item)
            }
        }
        Unit
    }

    /** 移除一个工作区（只从列表里去掉，不会删除磁盘上的任何文件）。 */
    suspend fun remove(path: String): Result<Unit> = io {
        val target = normalizePath(path)
        context.workspaceDataStore.edit { prefs ->
            val current = decode(prefs[KEY].orEmpty())
            val remaining = current.filterNot { it.path == target }
            if (remaining.size != current.size) {
                prefs[KEY] = encode(remaining)
            }
        }
        Unit
    }

    /** 清空工作区列表。 */
    suspend fun clear(): Result<Unit> = io {
        context.workspaceDataStore.edit { prefs -> prefs.remove(KEY) }
        Unit
    }

    /** 某个路径是否已经在工作区列表里。 */
    suspend fun contains(path: String): Boolean =
        io {
            val target = normalizePath(path)
            workspaces.first().any { it.path == target }
        }.getOrDefault(false)

    // ======================================================================
    // 内部工具
    // ======================================================================

    private suspend fun <T> io(block: suspend () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    /** 统一路径写法：去掉首尾空格与斜杠，反斜杠换成斜杠。 */
    private fun normalizePath(path: String): String =
        path.trim().replace('\\', '/').trim('/')

    /**
     * 保存格式：一个 JSON 数组，例如
     * [{"path":"notes","type":"DIRECTORY"},{"path":"notes/a.txt","type":"FILE"}]
     * 用 JSON 是因为路径里可能出现各种奇怪字符，用逗号或换行拼接会出错。
     */
    private fun encode(items: List<WorkspaceItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put(FIELD_PATH, item.path)
                    .put(FIELD_TYPE, item.type.name),
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<WorkspaceItem> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    if (!obj.has(FIELD_PATH)) continue
                    val path = obj.optString(FIELD_PATH)
                    val type = runCatching { WorkspaceType.valueOf(obj.optString(FIELD_TYPE)) }
                        .getOrDefault(WorkspaceType.DIRECTORY)
                    add(WorkspaceItem.fromPath(path, type))
                }
            }
        } catch (_: Throwable) {
            // 存储内容损坏时不要让应用崩溃：当作空列表处理
            emptyList()
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("workspace_items")
        const val FIELD_PATH = "path"
        const val FIELD_TYPE = "type"
    }
}
