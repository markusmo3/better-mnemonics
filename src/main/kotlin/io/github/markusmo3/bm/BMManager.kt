package io.github.markusmo3.bm

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManager
import gnu.trove.THashSet
import io.github.markusmo3.bm.config.BMActionsSchema

class BMManager(
  private val myActionManager: ActionManager, private val keymapManager: KeymapManager
) : BaseComponent {

  private val bmActionPrefix = "InvokeBMPopupAction_"

  override fun initComponent() {
    registerActions()
  }

  private fun registerActions() {
    InvokeBMPopupAction.setNodeKeyStroke = false
    val bmActionsSchema = BMActionsSchema.getInstance()
    bmActionsSchema.bmManager = this
    val registeredIds = THashSet<String>()
    for (root in bmActionsSchema.state.root) {
      if (!root.isGroup()) continue
      val actionId = bmActionPrefix + root.actionId + "_" + root.customText
      if (registeredIds.add(actionId)) {
        myActionManager.registerAction(actionId, InvokeBMPopupAction(root), pluginId)

        val keymap: Keymap? = keymapManager.getKeymap("\$default")
        val keyStroke = root.keyStroke
        if (keymap != null && keyStroke != null) {
          keymap.removeAllActionShortcuts(actionId)
          keymap.addShortcut(actionId, KeyboardShortcut(keyStroke, null))
        }
      }
    }
    InvokeBMPopupAction.setNodeKeyStroke = true
  }

  private fun unregisterActions() {
    for (oldId in myActionManager.getActionIds(bmActionPrefix)) {
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
