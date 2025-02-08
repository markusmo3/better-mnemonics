package io.github.markusmo3.bm.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.impl.stores.stateStore
import io.github.markusmo3.bm.config.BMActionsSchema

class ReloadBMXmlAction : AnAction("Reload BetterMnemonics XML", "Reload BetterMnemonics XML", AllIcons.Actions.ForceRefresh) {

    @Suppress("UnstableApiUsage")
    override fun actionPerformed(e: AnActionEvent) {
        ApplicationManager.getApplication().stateStore.reloadState(BMActionsSchema::class.java)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
