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
     * Warning: The first access to this service will trigger `loadState()`.
     * If the persistent state contains a property of type `PropertyType.SECRET`,
     * the `PasswordSafe` will be called to retrieve the secret.
     * This call will check if it's not called on the EDT, therefore, the first
     * call to this method must be done on a background thread.
     * Note that this is not the case for the dialog, since it is opened by an
     * action and actions are exempt from the EDT check.
     */
    fun getInstance(project: Project): DeveloperToolsToolWindowSettings =
      project.getService(DeveloperToolsToolWindowSettings::class.java)
  }
}