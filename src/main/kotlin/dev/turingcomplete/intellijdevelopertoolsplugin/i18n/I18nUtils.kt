package dev.turingcomplete.intellijdevelopertoolsplugin.i18n

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.local"

object I18nUtils : DynamicBundle(BUNDLE) {

  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)

}