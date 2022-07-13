package io.github.markusmo3.bm

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator

class BMListener : PreloadingActivity(), DynamicPluginListener {
  private var firstInitializationOccurred = false

  override fun preload(indicator: ProgressIndicator) {
    if (firstInitializationOccurred) return
    firstInitializationOccurred = true
    BMManager.getInstance().registerActions()
  }

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    if (isMyPlugin(pluginDescriptor)) {
      BMManager.getInstance().reset()
    }
  }

  override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
    if (isMyPlugin(pluginDescriptor)) {
      BMManager.getInstance().dispose()
    }
  }

  private fun isMyPlugin(pluginDescriptor: IdeaPluginDescriptor): Boolean {
    return pluginDescriptor.pluginId == BMManager.pluginId
  }

}
