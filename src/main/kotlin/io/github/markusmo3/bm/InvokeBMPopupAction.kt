package io.github.markusmo3.bm

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopup
import io.github.markusmo3.bm.config.BMNode
import io.github.markusmo3.bm.popup.BMActionGroupPopup

class InvokeBMPopupAction(private val node: BMNode) : AnAction(), DumbAware {

	init {
		templatePresentation.description = "Shows the '" + node.customText + "' popup."
		templatePresentation.setText(node.customText, false)
	}

	override fun actionPerformed(e: AnActionEvent) {
		val popup = BMActionGroupPopup(node, e.dataContext)
		popup.show(e.dataContext)
	}

	private fun JBPopup.show(dataContext: DataContext? = null) {
		val contextComponent = dataContext?.getData(PlatformDataKeys.CONTEXT_COMPONENT)
		if (contextComponent != null) {
			showInCenterOf(contextComponent)
		} else {
			showInFocusCenter()
		}
	}

	override fun setShortcutSet(shortcutSet: ShortcutSet) {
		super.setShortcutSet(shortcutSet)
		if (setNodeKeyStroke) {
			val keyStrokeToSet = shortcutSet.shortcuts.filter { it.isKeyboard }
					.map { it as KeyboardShortcut }
					.filter { it.secondKeyStroke == null }
					.map { it.firstKeyStroke }
					.firstOrNull()
			node.keyStroke = keyStrokeToSet
		}
	}

	companion object {
		var setNodeKeyStroke = true
	}

}
