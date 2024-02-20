package io.github.markusmo3.bm

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class BMListener : DynamicPluginListener, ProjectActivity {

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

  override suspend fun execute(project: Project) {
    BMManager.getInstance().reset()
  }

}
