package io.github.markusmo3.bm

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PluginStartup : StartupActivity.DumbAware {

  private var firstInitializationOccurred = false

  override fun runActivity(project: Project) {
    if (firstInitializationOccurred) return
    firstInitializationOccurred = true
    BMManager.getInstance().registerActions()
  }
}