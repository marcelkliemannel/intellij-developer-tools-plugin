package dev.turingcomplete.intellijdevelopertoolsplugin.settings.message

import com.intellij.DynamicBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle.ID
import org.jetbrains.annotations.PropertyKey

object SettingsBundle : DynamicBundle(SettingsBundle::class.java, ID) {
  // -- Properties ---------------------------------------------------------- //

  const val ID = "message.SettingsBundle"

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = ID) key: String, vararg params: Any): String =
    getMessage(key, *params)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
