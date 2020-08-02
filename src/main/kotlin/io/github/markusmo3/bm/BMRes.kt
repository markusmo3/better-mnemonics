package io.github.markusmo3.bm

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

class BMRes : DynamicBundle(BUNDLE) {

  companion object {
    const val BUNDLE = "BMResources"
    private val INSTANCE = BMRes()

    fun <T : Any?> get(clazz: Class<T>, key: String, vararg params: Any): String {
      return get(clazz.simpleName + "." + key, *params)
    }

    fun get(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
      return INSTANCE.getMessage(key, *params)
    }

  }
}