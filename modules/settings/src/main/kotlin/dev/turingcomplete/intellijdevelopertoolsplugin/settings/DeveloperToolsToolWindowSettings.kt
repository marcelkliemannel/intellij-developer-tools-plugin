package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level.PROJECT
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(
  name = "DeveloperToolsToolWindowSettingsV1",
  storages = [Storage("developer-tools.xml")],
  category = SettingsCategory.TOOLS
)
@Service(PROJECT)
class DeveloperToolsToolWindowSettings : DeveloperToolsInstanceSettings() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    /**
     * Warning: The first access to this service will trigger the `loadState()`.
     */
    fun getInstance(project: Project): DeveloperToolsToolWindowSettings =
      project.getService(DeveloperToolsToolWindowSettings::class.java)
  }
}
