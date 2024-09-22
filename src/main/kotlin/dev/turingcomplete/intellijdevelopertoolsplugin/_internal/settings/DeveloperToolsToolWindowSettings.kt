package dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level.PROJECT
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsInstanceSettings.InstanceState

@State(
  name = "DeveloperToolsToolWindowSettingsV1",
  storages = [Storage("developer-tools.xml")],
  category = SettingsCategory.TOOLS
)
@Service(PROJECT)
internal class DeveloperToolsToolWindowSettings :
  DeveloperToolsInstanceSettings(),
  PersistentStateComponent<InstanceState> {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    /**
     * Warning: The first access to this service will trigger the `loadState()`.
     */
    fun getInstance(project: Project): DeveloperToolsToolWindowSettings =
      project.getService(DeveloperToolsToolWindowSettings::class.java)
  }
}