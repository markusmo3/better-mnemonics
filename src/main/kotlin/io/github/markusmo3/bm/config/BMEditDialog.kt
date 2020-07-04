package io.github.markusmo3.bm.config

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.GridBag
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.KeyStroke

internal class BMEditDialog(
  private val isShortcutEditingEnabled: Boolean,
  private val isGlobalShortcutEditingEnabled: Boolean,
  private val oldBmNode: BMNode?
) : DialogWrapper(false) {

  init {
    title = "Edit"
    init()
  }

  private lateinit var myPanel: BMEditPanel

  override fun createCenterPanel(): JComponent? {
    myPanel = BMEditPanel(isShortcutEditingEnabled, isGlobalShortcutEditingEnabled, oldBmNode)
    return myPanel
  }

  fun getCustomText(): String? {
    val customText = myPanel.getCustomText()
    if (customText.isNullOrEmpty()) {
      return null
    }
    return customText
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    if (isShortcutEditingEnabled) {
      return myPanel.myShortcutTextfield
    } else {
      return myPanel.myCustomTextTextfield
    }
  }

  fun getKeyStroke(): KeyStroke? {
    return myPanel.getKeyStroke()
  }

  fun getGlobalKeyStroke(): KeyStroke? {
    return myPanel.getGlobalKeyStroke()
  }
}

internal class BMEditPanel(
  isShortcutEditingEnabled: Boolean,
  isGlobalShortcutEditingEnabled: Boolean,
  oldBmNode: BMNode?
) : JPanel(GridBagLayout()) {

  internal var myShortcutTextfield: BMKeyStrokeTextfield = BMKeyStrokeTextfield()
  internal var myCustomTextTextfield: JBTextField = JBTextField()
  internal var myGlobalShortcutTextfield: BMKeyStrokeTextfield = BMKeyStrokeTextfield()

  init {
    oldBmNode?.let {
      myCustomTextTextfield.text = it.customText
      myShortcutTextfield.setKeyStroke(oldBmNode.keyStroke)
      myGlobalShortcutTextfield.setKeyStroke(oldBmNode.globalKeyStroke)
    }

    val gridBag = GridBag().setDefaultAnchor(0, GridBag.EAST).setDefaultInsets(0, 0, 0, 0, 6)
      .setDefaultAnchor(1, GridBag.WEST).setDefaultInsets(1, 0, 0, 0, 0).setDefaultWeightX(1, 1.0)
      .setDefaultFill(GridBag.HORIZONTAL)
    if (isShortcutEditingEnabled) {
      this.add(createLabelFor(myShortcutTextfield, "KeyStroke"), gridBag.nextLine().next())
      this.add(myShortcutTextfield, gridBag.next())
    }
    this.add(createLabelFor(myCustomTextTextfield, "Custom Text"), gridBag.nextLine().next())
    this.add(myCustomTextTextfield, gridBag.next())
    if (isGlobalShortcutEditingEnabled) {
      this.add(
        createLabelFor(myGlobalShortcutTextfield, "Global KeyStroke"),
        gridBag.nextLine().next()
      )
      this.add(myGlobalShortcutTextfield, gridBag.next())
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
    return myShortcutTextfield.getKeyStroke()
  }

  fun getGlobalKeyStroke(): KeyStroke? {
    return myGlobalShortcutTextfield.getKeyStroke()
  }

  fun createLabelFor(component: JComponent?, text: String?): JLabel {
    val label = JLabel(text)
    label.labelFor = component
    return label
  }
}
