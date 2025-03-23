package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
  name = "DeveloperToolsDialogSettingsV1",
  storages = [Storage("developer-tools.xml")],
  category = SettingsCategory.TOOLS
)
class DeveloperToolsDialogSettings : DeveloperToolsInstanceSettings() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    val instance: DeveloperToolsDialogSettings
      get() = ApplicationManager.getApplication().getService(DeveloperToolsDialogSettings::class.java)
  }
}
