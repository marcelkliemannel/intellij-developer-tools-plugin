package dev.turingcomplete.intellijdevelopertoolsplugin.settings.message

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

object SettingsBundle {
  // -- Properties ---------------------------------------------------------- //

  const val SETTINGS_BUNDLE_ID = "message.SettingsBundle"

  private val instance: DynamicBundle = DynamicBundle(SettingsBundle::class.java, SETTINGS_BUNDLE_ID)

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = SETTINGS_BUNDLE_ID) key: String, vararg params: Any): String =
    instance.getMessage(key, *params)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
