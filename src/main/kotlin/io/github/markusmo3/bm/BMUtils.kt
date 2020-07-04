package io.github.markusmo3.bm

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.keymap.impl.ui.ShortcutTextField
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color
import java.awt.color.ColorSpace
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.im.InputContext
import java.io.File
import java.util.*
import javax.swing.KeyStroke

object BMUtils {

  private val customKeyCodeToStringMap = mapOf(
    KeyEvent.VK_ENTER to "RET",
    KeyEvent.VK_SPACE to "SPC",
    KeyEvent.VK_DELETE to "DEL",
    KeyEvent.VK_TAB to "TAB",
    KeyEvent.VK_BACK_SPACE to "BS",
    KeyEvent.VK_ESCAPE to "ESC"
  )

  fun KeyStroke?.toSortIndex(): Long {
    if (this == null) return -1
    val lowerSort = if (keyCode == 0) {
      keyChar.toInt()
    } else {
      keyCode
    }
    return modifiers.toLong().shl(32).or(lowerSort.toLong())
  }

  fun KeyStroke?.toShortString(): String {
    if (this == null) return "<null>"
    val sj = StringJoiner("-")

    InputContext.getInstance().inputMethodControlObject

    val isAtoZ = keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z
    val isShiftPressed = modifiers and InputEvent.SHIFT_DOWN_MASK != 0

    if (modifiers and InputEvent.ALT_DOWN_MASK != 0) sj.add("A")
    if (modifiers and InputEvent.CTRL_DOWN_MASK != 0) sj.add("C")
    if (modifiers and InputEvent.ALT_GRAPH_DOWN_MASK != 0) sj.add("G")
    if (modifiers and InputEvent.META_DOWN_MASK != 0) sj.add("M")
    if (isShiftPressed && !isAtoZ) sj.add("S")
    if (modifiers and InputEvent.BUTTON1_DOWN_MASK != 0) sj.add("m1")
    if (modifiers and InputEvent.BUTTON2_DOWN_MASK != 0) sj.add("m2")
    if (modifiers and InputEvent.BUTTON3_DOWN_MASK != 0) sj.add("m3")
    if (keyCode == KeyEvent.VK_UNDEFINED) {
      sj.add(keyChar.toString())
    } else if (isAtoZ) {
      if (isShiftPressed) {
        sj.add(KeyEvent.getKeyText(keyCode).toUpperCase())
      } else {
        sj.add(KeyEvent.getKeyText(keyCode).toLowerCase())
      }
    } else if (customKeyCodeToStringMap.containsKey(keyCode)) {
      sj.add(customKeyCodeToStringMap[keyCode])
    } else {
      sj.add(KeyEvent.getKeyText(keyCode))
    }
    return sj.toString()
  }

  fun getPluginPath(): File? {
    var path: File = PluginManager.getPlugin(BMManager.pluginId)!!.path
    if (path.name.endsWith(".jar")) {
      path = File(path.parentFile, path.name.substring(0, path.name.length - 4))
    }
    return File(path.parentFile, path.name + "_templates")
  }

  fun openFileInEditor(project: Project, file: File) {
    var vFile = LocalFileSystem.getInstance().findFileByIoFile(file)
    if (vFile == null) {
      LocalFileSystem.getInstance().refreshIoFiles(arrayListOf(file))
      vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    }
    if (vFile != null) {
      openFileInEditor(project, vFile)
    }
  }

  fun openFileInEditor(project: Project, vFile: VirtualFile) {
    val openFileDescriptor = OpenFileDescriptor(project, vFile)
    openFileDescriptor.navigate(true)
  }

  @JvmStatic
  fun ShortcutTextField.getKeyStrokeKt(): KeyStroke? {
    val getKeyStrokeMethod = ShortcutTextField::class.java.getDeclaredMethod("getKeyStroke")
    if (!getKeyStrokeMethod.isAccessible) {
      getKeyStrokeMethod.isAccessible = true
    }
    return getKeyStrokeMethod.invoke(this) as KeyStroke?
  }

  @JvmStatic
  fun ShortcutTextField?.setKeyStrokeKt(keyStroke: KeyStroke?) {
    val setKeyStrokeMethod =
      ShortcutTextField::class.java.getDeclaredMethod("setKeyStroke", KeyStroke::class.java)
    if (!setKeyStrokeMethod.isAccessible) {
      setKeyStrokeMethod.isAccessible = true
    }
    setKeyStrokeMethod.invoke(this, keyStroke) as KeyStroke?
  }

  fun Color.blend(that: Color, ratio: Double = 0.5): Color {
    val cRatio = ratio.coerceIn(0.0, 1.0).toFloat()
    val iRatio = (1.0 - cRatio).toFloat()

    val thisComps = this.getRGBComponents(null)
    val thatComps = that.getRGBComponents(null)
    thisComps[0] = (thisComps[0] * iRatio + thatComps[0] * cRatio)
    thisComps[1] = (thisComps[1] * iRatio + thatComps[1] * cRatio)
    thisComps[2] = (thisComps[2] * iRatio + thatComps[2] * cRatio)
    thisComps[3] = (thisComps[3] * iRatio + thatComps[3] * cRatio)

    return Color(ColorSpace.getInstance(ColorSpace.CS_sRGB), thisComps, thisComps[3])
  }
}