package io.github.markusmo3.bm.config

import com.intellij.openapi.options.Configurable.NoScroll
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class BMActionsConfigurable : SearchableConfigurable, NoScroll {
  private var myPanel: BMActionsConfigurablePanel? = null

  override fun createComponent(): JComponent? {
    if (myPanel == null) {
      myPanel = BMActionsConfigurablePanel()
    }
    return myPanel!!.panel
  }

  override fun getDisplayName(): String {
    return "Better Mnemonics"
  }

  override fun getHelpTopic(): String? {
    return id
  }

  @Throws(ConfigurationException::class)
  override fun apply() {
    myPanel!!.apply()
  }

  override fun reset() {
    myPanel!!.reset(true)
  }

  override fun isModified(): Boolean {
    return myPanel?.isModified ?: false
  }

  override fun getId(): String {
    return "preferences.BMActionsConfigurable"
  }
}

