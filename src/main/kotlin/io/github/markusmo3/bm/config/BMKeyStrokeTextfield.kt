package io.github.markusmo3.bm.config

import com.intellij.icons.AllIcons
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.accessibility.ScreenReader
import io.github.markusmo3.bm.BMUtils.toShortString
import java.awt.AWTEvent
import java.awt.AWTKeyStroke
import java.awt.KeyboardFocusManager
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.*
import java.util.function.Consumer
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.KeyStroke
import javax.swing.text.DefaultCaret

/**
 * @see com.intellij.openapi.keymap.impl.ui.ShortcutTextField
 */
class BMKeyStrokeTextfield(isFocusTraversalKeysEnabled: Boolean = true) :
  ExtendableTextField() {

  private var myKeyStroke: KeyStroke? = null
  private var myLastPressedKeyCode = KeyEvent.VK_UNDEFINED

  init {
    enableEvents(AWTEvent.KEY_EVENT_MASK)
    focusTraversalKeysEnabled = isFocusTraversalKeysEnabled
    if (isFocusTraversalKeysEnabled) {
      setExtensions(ExtendableTextComponent.Extension.create(
        AllIcons.General.InlineAdd,
        AllIcons.General.InlineAddHover,
        getPopupTooltip()
      ) { this.showPopup() })
    }
    caret = object : DefaultCaret() {
      override fun isVisible(): Boolean {
        return false
      }
    }
    columns = 13
  }

  private fun absolutelyUnknownKey(e: KeyEvent): Boolean {
    return e.keyCode == 0
        && e.keyChar == KeyEvent.CHAR_UNDEFINED
        && e.keyLocation == KeyEvent.KEY_LOCATION_UNKNOWN
        && e.extendedKeyCode == 0
  }

  override fun processKeyEvent(e: KeyEvent) {
    val keyCode = e.keyCode
    if (focusTraversalKeysEnabled && e.modifiers == 0 && e.modifiersEx == 0) {
      if (keyCode == KeyEvent.VK_BACK_SPACE) {
        setKeyStroke(null)
        return
      }
      if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER && myKeyStroke != null) {
        super.processKeyEvent(e)
        return
      }
    }
    val isNotModifierKey = keyCode != KeyEvent.VK_SHIFT
        && keyCode != KeyEvent.VK_ALT
        && keyCode != KeyEvent.VK_CONTROL
        && keyCode != KeyEvent.VK_ALT_GRAPH
        && keyCode != KeyEvent.VK_META
        && !absolutelyUnknownKey(e)
    if (isNotModifierKey) {
      // NOTE: when user presses 'Alt + Right' at Linux the IDE can receive next sequence KeyEvents: ALT_PRESSED -> RIGHT_RELEASED ->  ALT_RELEASED
      // RIGHT_PRESSED can be skipped, it depends on WM
      if (e.id == KeyEvent.KEY_PRESSED
        || e.id == KeyEvent.KEY_RELEASED
        && SystemInfo.isLinux
        && (e.isAltDown || e.isAltGraphDown)
        && myLastPressedKeyCode != keyCode // press-event was skipped
      ) {
        setKeyStroke(KeyStrokeAdapter.getDefaultKeyStroke(e))
      }
      if (e.id == KeyEvent.KEY_PRESSED) myLastPressedKeyCode = keyCode
    }

    // Ensure TAB/Shift-TAB work as focus traversal keys, otherwise
    // there is no proper way to move the focus outside the text field.
    if (!focusTraversalKeysEnabled && ScreenReader.isActive()) {
      focusTraversalKeysEnabled = true
      try {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().processKeyEvent(this, e)
      } finally {
        focusTraversalKeysEnabled = false
      }
    }
  }

  fun setKeyStroke(keyStroke: KeyStroke?) {
    val old = myKeyStroke
    if (old != null || keyStroke != null) {
      myKeyStroke = keyStroke
      if (keyStroke != null) {
        super.setText(keyStroke.toShortString())
      } else {
        super.setText(null)
      }
      caretPosition = 0
      firePropertyChange("keyStroke", old, keyStroke)
    }
  }

  fun getKeyStroke(): KeyStroke? {
    return myKeyStroke
  }

  override fun enableInputMethods(enable: Boolean) {
    super.enableInputMethods(enable && Registry.`is`("ide.settings.keymap.input.method.enabled"))
  }

  override fun setText(text: String?) {
    super.setText(text)
    caretPosition = 0
    if (text == null || text.isEmpty()) {
      myKeyStroke = null
      firePropertyChange("keyStroke", null, null)
    }
  }

  private fun showPopup() {
    val menu = JBPopupMenu()
    getKeyStrokes().forEach(Consumer { stroke: KeyStroke ->
      menu.add(
        getPopupAction(stroke)
      )
    })
    val insets = insets
    menu.show(this, width - insets.right, insets.top)
  }

  private fun getPopupAction(stroke: KeyStroke): Action {
    return object : AbstractAction("Set " + KeymapUtil.getKeystrokeText(stroke)) {
      override fun actionPerformed(event: ActionEvent) {
        setKeyStroke(stroke)
      }
    }
  }

  private fun getPopupTooltip(): String {
    val sb = StringBuilder()
    var prefix = "Set shortcuts with "
    for (stroke in getKeyStrokes()) {
      if (0 == stroke.modifiers) {
        sb.append(prefix).append(KeymapUtil.getKeystrokeText(stroke))
        prefix = ", "
      }
    }
    return sb.append(" keys").toString()
  }

  private fun getKeyStrokes(): Iterable<KeyStroke> {
    val list = ArrayList<KeyStroke>()
    addKeyStrokes(list, getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS))
    addKeyStrokes(list, getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS))
    addKeyStrokes(list, getFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS))
    addKeyStrokes(list, getFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS))
    list.add(0, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    list.add(1, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0))
    list.add(2, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))
    return list
  }

  private fun addKeyStrokes(
    list: ArrayList<in KeyStroke>, strokes: Iterable<AWTKeyStroke>?
  ) {
    if (strokes != null) {
      for (stroke in strokes) {
        val keyCode = stroke.keyCode
        if (keyCode != KeyEvent.VK_UNDEFINED) {
          list.add(
            if (stroke is KeyStroke) stroke else KeyStroke.getKeyStroke(
              keyCode, stroke.modifiers, stroke.isOnKeyRelease
            )
          )
        }
      }
    }
  }
}