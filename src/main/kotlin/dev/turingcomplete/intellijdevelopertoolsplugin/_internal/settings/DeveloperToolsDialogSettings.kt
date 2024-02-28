package dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsDialogSettings.DialogState

@State(
  name = "DeveloperToolsDialogSettingsV1",
  storages = [Storage("developer-tools.xml")],
  category = SettingsCategory.TOOLS
)
internal class DeveloperToolsDialogSettings :
  DeveloperToolsInstanceSettings(),
  PersistentStateComponent<DialogState> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  var dialogIsModal: Boolean by ValueProperty(DIALOG_IS_MODAL_DEFAULT)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getState(): DialogState {
    val instanceState = super.getState()
    return DialogState(
      dialogIsModal = dialogIsModal,
      developerToolsConfigurations = instanceState.developerToolsConfigurations,
      lastSelectedContentNodeId = instanceState.lastSelectedContentNodeId,
      expandedGroupNodeIds = instanceState.expandedGroupNodeIds
    )
  }

  override fun loadState(state: DialogState) {
    super.loadState(state)
    dialogIsModal = (state.dialogIsModal ?: DIALOG_IS_MODAL_DEFAULT)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @Property(assertIfNoBindings = false)
  class DialogState(
    @get:Attribute("dialogIsModal")
    var dialogIsModal: Boolean? = null,
    developerToolsConfigurations: List<DeveloperToolConfigurationState>? = null,
    lastSelectedContentNodeId: String? = null,
    expandedGroupNodeIds: List<String>? = null
  ) : InstanceState(
    developerToolsConfigurations = developerToolsConfigurations,
    lastSelectedContentNodeId = lastSelectedContentNodeId,
    expandedGroupNodeIds = expandedGroupNodeIds
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val instance: DeveloperToolsDialogSettings
      get() = ApplicationManager.getApplication().getService(DeveloperToolsDialogSettings::class.java)

    const val DIALOG_IS_MODAL_DEFAULT = false
  }
}