package io.github.markusmo3.bm.config

import com.intellij.openapi.keymap.impl.ui.ShortcutTextField
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.GridBag
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.KeyStroke

internal class BMEditDialog(
  private val isShortcutEditingEnabled: Boolean, private val oldBmNode: BMNode?
) : DialogWrapper(false) {

  init {
    title = "Edit"
    init()
  }

  private lateinit var myPanel: BMEditPanel

  override fun createCenterPanel(): JComponent? {
    myPanel = BMEditPanel(isShortcutEditingEnabled, oldBmNode)
    return myPanel
  }

  fun getCustomText(): String? {
    val customText = myPanel.getCustomText()
    if (customText.isNullOrEmpty()) {
      return null
    }
    return customText
  }

  fun getKeyStroke(): KeyStroke? {
    return myPanel.getKeyStroke()
  }
}

internal class BMEditPanel(
  isShortcutEditingEnabled: Boolean, private val oldBmNode: BMNode?
) : JPanel(GridBagLayout()) {

  private var myCustomTextTextfield: JBTextField = JBTextField()
  private var myShortcutTextfield: ShortcutTextField? = null

  init {
    if (isShortcutEditingEnabled) {
      val firstConstructor = ShortcutTextField::class.java.declaredConstructors.first()
      if (!firstConstructor.isAccessible) {
        firstConstructor.isAccessible = true
      }
      myShortcutTextfield = firstConstructor.newInstance(true) as ShortcutTextField?
      myShortcutTextfield?.columns = 13
    }

    oldBmNode?.let {
      myCustomTextTextfield.text = it.customText
      myShortcutTextfield?.setKeyStrokeKt(oldBmNode.keyStroke)
    }

    val gridBag = GridBag().setDefaultAnchor(0, GridBag.EAST).setDefaultInsets(0, 0, 0, 0, 6)
      .setDefaultAnchor(1, GridBag.WEST).setDefaultInsets(1, 0, 0, 0, 0).setDefaultWeightX(1, 1.0)
      .setDefaultFill(GridBag.HORIZONTAL)
    this.add(createLabelFor(myCustomTextTextfield, "Custom Text"), gridBag.nextLine().next())
    this.add(myCustomTextTextfield, gridBag.next())
    if (isShortcutEditingEnabled) {
      this.add(createLabelFor(myShortcutTextfield, "KeyStroke"), gridBag.nextLine().next())
      this.add(myShortcutTextfield, gridBag.next())
    }
  }

  fun getCustomText(): String? {
    if (myCustomTextTextfield.text.isBlank()) {
      return null
    } else {
      return myCustomTextTextfield.text
    }
  }

  fun getKeyStroke(): KeyStroke? {
    return myShortcutTextfield?.getKeyStrokeKt()
  }

  fun createLabelFor(component: JComponent?, text: String?): JLabel {
    val label = JLabel(text)
    label.labelFor = component
    return label
  }

  private fun ShortcutTextField.getKeyStrokeKt(): KeyStroke? {
    val getKeyStrokeMethod = ShortcutTextField::class.java.getDeclaredMethod("getKeyStroke")
    if (!getKeyStrokeMethod.isAccessible) {
      getKeyStrokeMethod.isAccessible = true
    }
    return getKeyStrokeMethod.invoke(this) as KeyStroke?
  }

  private fun ShortcutTextField?.setKeyStrokeKt(keyStroke: KeyStroke?) {
    val setKeyStrokeMethod =
      ShortcutTextField::class.java.getDeclaredMethod("setKeyStroke", KeyStroke::class.java)
    if (!setKeyStrokeMethod.isAccessible) {
      setKeyStrokeMethod.isAccessible = true
    }
    setKeyStrokeMethod.invoke(this, keyStroke) as KeyStroke?
  }

}
