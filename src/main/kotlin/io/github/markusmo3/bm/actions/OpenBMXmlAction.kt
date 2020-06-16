package io.github.markusmo3.bm.actions

import com.intellij.configurationStore.StateStorageManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.stateStore
import io.github.markusmo3.bm.BMUtils
import java.nio.file.Paths

class OpenBMXmlAction : AnAction("Open BetterMnemonics xml", null, AllIcons.FileTypes.Xml) {
  init {
    templatePresentation.isEnabled = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val storageManager: StateStorageManager =
      ApplicationManager.getApplication().stateStore.storageManager
    val location = Paths.get(storageManager.expandMacros("betterMnemonicsSchema.xml"))
    BMUtils.openFileInEditor(e.project!!, location.toFile())
  }

  override fun update(e: AnActionEvent) {
    templatePresentation.isEnabled = true
  }
}