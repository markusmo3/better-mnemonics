package io.github.markusmo3.bm.config

import com.intellij.util.xmlb.Converter
import javax.swing.KeyStroke

internal class KeyStrokeConverter : Converter<KeyStroke>() {
  override fun toString(value: KeyStroke): String? {
    return value.toString()
  }

  override fun fromString(value: String): KeyStroke? {
    return KeyStroke.getKeyStroke(value)
  }
}