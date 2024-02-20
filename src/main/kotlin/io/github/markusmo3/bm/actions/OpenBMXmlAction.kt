package io.github.markusmo3.bm.actions

import com.intellij.configurationStore.StateStorageManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.stateStore
import io.github.markusmo3.bm.BMUtils

class OpenBMXmlAction : AnAction() {

    @Suppress("UnstableApiUsage")
    override fun actionPerformed(e: AnActionEvent) {
        val storageManager: StateStorageManager =
            ApplicationManager.getApplication().stateStore.storageManager
        val location = storageManager.expandMacro("betterMnemonicsSchema.xml")
        BMUtils.openFileInEditor(e.project!!, location.toFile())
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
