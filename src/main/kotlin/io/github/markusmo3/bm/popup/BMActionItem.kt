package io.github.markusmo3.bm.popup

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ShortcutProvider
import com.intellij.openapi.actionSystem.ShortcutSet
import io.github.markusmo3.bm.config.BMNode
import java.beans.PropertyChangeEvent
import javax.swing.Icon
import javax.swing.KeyStroke

data class BMActionItem(
  val bmNode: BMNode,
  val action: AnAction,
  var text: String,
  val description: String?,
  val isEnabled: Boolean,
  private val myIcon: Icon?,
  private val mySelectedIcon: Icon?,
  val isPrependWithSeparator: Boolean,
  val separatorText: String?,
  val keyStroke: KeyStroke?
) : ShortcutProvider {

  init {
    action.templatePresentation.addPropertyChangeListener { evt: PropertyChangeEvent ->
      if (evt.propertyName === Presentation.PROP_TEXT) {
        text = action.templatePresentation.text
      }
    }
  }

  fun getIcon(selected: Boolean): Icon? {
    return if (selected && mySelectedIcon != null) mySelectedIcon else myIcon
  }

  override fun getShortcut(): ShortcutSet? {
    return action.shortcutSet
  }

  override fun toString(): String {
    return text
  }
}