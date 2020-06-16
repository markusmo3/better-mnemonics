package io.github.markusmo3.bm.actions

import com.intellij.configurationStore.StateStorageManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.stateStore
import io.github.markusmo3.bm.config.BMActionsSchema
import java.nio.file.Paths

class ReloadBMXmlAction :
  AnAction("Reload BetterMnemonics xml", null, AllIcons.Actions.ForceRefresh) {

  init {
    templatePresentation.isEnabled = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val storageManager: StateStorageManager =
      ApplicationManager.getApplication().stateStore.storageManager
    val location = Paths.get(storageManager.expandMacros("betterMnemonicsSchema.xml"))
    BMActionsSchema.getInstance().loadStateFromFile(location.toFile())
  }

  override fun update(e: AnActionEvent) {
    templatePresentation.isEnabled = true
  }
}
