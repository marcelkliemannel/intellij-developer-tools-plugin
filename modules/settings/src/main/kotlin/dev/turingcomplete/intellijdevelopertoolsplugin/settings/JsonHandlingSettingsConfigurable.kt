package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.openapi.util.NlsContexts
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.jsonHandling
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsConfigurable
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle

class JsonHandlingSettingsConfigurable :
  SettingsConfigurable<JsonHandlingSettings>(settings = jsonHandling) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getDisplayName(): @NlsContexts.ConfigurableName String? =
    SettingsBundle.message("json-handling-settings.title")

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
