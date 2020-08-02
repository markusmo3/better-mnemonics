package io.github.markusmo3.bm

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.project.Project

class BMListener : AppLifecycleListener {
  override fun appStarting(projectFromCommandLine: Project?) {
    BMManager.getInstance().reset()
  }
}