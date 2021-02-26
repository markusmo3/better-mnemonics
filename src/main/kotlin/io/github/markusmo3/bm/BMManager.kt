package io.github.markusmo3.bm

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import io.github.markusmo3.bm.config.BMActionsSchema
import io.github.markusmo3.bm.config.BMActionsSchemaState
import io.github.markusmo3.bm.config.BMNode

@Service
class BMManager : Disposable {

  private val keymapManager: KeymapManager by lazy { KeymapManager.getInstance() }

  internal fun registerActions(actionSchemaState: BMActionsSchemaState = BMActionsSchema.getInstance().state) {
    InvokeBMPopupAction.setNodeKeyStroke = false
    val registeredIds = HashSet<String>()
    val parent = actionSchemaState.root
    registerShortcuts(parent, registeredIds)
    InvokeBMPopupAction.setNodeKeyStroke = true
  }

  private fun registerShortcuts(
    parent: BMNode, registeredIds: HashSet<String>
  ) {
    for (bmNode in parent) {
      if (!bmNode.isGroup()) continue
      if (bmNode.globalKeyStroke == null) {
        // maybe some childs are globally assignable
        registerShortcuts(bmNode, registeredIds)
        continue
      }

      val actionId = bmNode.actionIdForKeymap
      if (registeredIds.add(actionId)) {
        ActionManager.getInstance().registerAction(actionId, InvokeBMPopupAction(bmNode), pluginId)
        val keymap: Keymap? = keymapManager.getKeymap("\$default")
        val keyStroke = bmNode.globalKeyStroke
        if (keymap != null && keyStroke != null) {
          keymap.removeAllActionShortcuts(actionId)
          keymap.addShortcut(actionId, KeyboardShortcut(keyStroke, null))
        }
      }
      registerShortcuts(bmNode, registeredIds)
    }
  }

  internal fun unregisterActions() {
    for (oldId in ActionManager.getInstance().getActionIdList(BMNode.bmActionIdForKeymapPrefix)) {
      ActionManager.getInstance().unregisterAction(oldId)
    }
  }

  fun reset(actionSchemaState: BMActionsSchemaState = BMActionsSchema.getInstance().state) {
    unregisterActions()
    registerActions(actionSchemaState)
  }

  companion object {
    val pluginId = PluginId.getId("io.github.markusmo3.bm.BetterMnemonics")

    fun getInstance(): BMManager = ServiceManager.getService(BMManager::class.java)
  }

  override fun dispose() {
    unregisterActions()
  }
}
