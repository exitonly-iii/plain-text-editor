package com.example.plaintexteditor

import android.app.Application
import android.content.Context
import com.example.plaintexteditor.data.repository.FileRepository
import com.example.plaintexteditor.data.workspace.WorkspaceStore

/**
 * 应用入口（在 AndroidManifest.xml 里用 android:name=".PlainTextEditorApp" 挂上）。
 *
 * 它只做一件事：把“仓库”做成全应用唯一的实例，放在这里等别人来取。
 * 下一阶段的界面只要这样就能拿到：
 * ```
 * val app = PlainTextEditorApp.from(context)
 * val items = app.fileRepository.list(currentPath)
 * ```
 */
class PlainTextEditorApp : Application() {

    /** 文件仓库：所有文件的读写都走它（根目录就是应用私有目录 filesDir） */
    val fileRepository: FileRepository by lazy {
        FileRepository(rootDirectory = filesDir)
    }

    /** 工作区存储：记住“我关心哪些目录/文件” */
    val workspaceStore: WorkspaceStore by lazy {
        WorkspaceStore(context = this)
    }

    companion object {
        /**
         * 从任何 Context 里拿到本应用实例。
         * 在 Android Studio 的“设计预览”里拿不到（返回 null），真机运行时一定拿得到。
         */
        fun from(context: Context): PlainTextEditorApp? =
            context.applicationContext as? PlainTextEditorApp
    }
}
