package io.github.markusmo3.bm

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import gnu.trove.THashSet
import io.github.markusmo3.bm.config.BMActionsSchema
import io.github.markusmo3.bm.config.BMNode

class BMManager(
  private val myActionManager: ActionManager, private val keymapManager: KeymapManager
) : BaseComponent {

  override fun initComponent() {
    registerActions()
  }

  private fun registerActions() {
    InvokeBMPopupAction.setNodeKeyStroke = false
    val bmActionsSchema = BMActionsSchema.getInstance()
    bmActionsSchema.bmManager = this
    val registeredIds = THashSet<String>()
    val parent = bmActionsSchema.state.root
    registerShortcuts(parent, registeredIds)
    InvokeBMPopupAction.setNodeKeyStroke = true
  }

  private fun registerShortcuts(
    parent: BMNode, registeredIds: THashSet<String>
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
        myActionManager.registerAction(actionId, InvokeBMPopupAction(bmNode), pluginId)
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

  private fun unregisterActions() {
    for (oldId in myActionManager.getActionIds(BMNode.bmActionIdForKeymapPrefix)) {
      myActionManager.unregisterAction(oldId)
    }
  }

  fun reset() {
    unregisterActions()
    registerActions()
  }

  companion object {
    val pluginId = PluginId.getId("io.github.markusmo3.bm.BetterMnemonics")
  }
}
