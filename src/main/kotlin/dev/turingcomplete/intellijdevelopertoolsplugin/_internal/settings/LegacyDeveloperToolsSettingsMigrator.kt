package dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XCollection
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings.ApplicationState
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.LegacyDeveloperToolsSettingsMigrator.LegacyState

@State(
  name = "DeveloperToolsPluginService",
  storages = [Storage("developer-tools.xml")]
)
internal class LegacyDeveloperToolsSettingsMigrator : PersistentStateComponent<LegacyState> {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getState(): LegacyState? = null

  override fun loadState(state: LegacyState) {
    DeveloperToolsDialogSettings.instance.loadState(
      DeveloperToolsDialogSettings.DialogState(
        dialogIsModal = state.dialogIsModal,
        developerToolsConfigurations = state.developerToolsConfigurations,
        lastSelectedContentNodeId = state.lastSelectedContentNodeId,
        expandedGroupNodeIds = state.expandedGroupNodeIds
      )
    )
    DeveloperToolsApplicationSettings.instance.loadState(
      ApplicationState(
        loadExamples = state.loadExamples,
        saveConfigurations = state.saveConfigurations,
        saveInputs = state.saveInputs,
        saveSecrets = state.saveSecrets,
        editorSoftWraps = state.editorSoftWraps,
        editorShowSpecialCharacters = state.editorShowSpecialCharacters,
        editorShowWhitespaces = state.editorShowWhitespaces
      )
    )
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class LegacyState(
    @get:XCollection(style = XCollection.Style.v2, elementName = "developerToolsConfigurations")
    var developerToolsConfigurations: List<DeveloperToolsInstanceSettings.DeveloperToolConfigurationState>? = null,
    @get:Attribute("lastSelectedContentNodeId")
    var lastSelectedContentNodeId: String? = null,
    @get:Attribute("loadExamples")
    var loadExamples: Boolean? = null,
    @get:Attribute("saveConfigurations")
    var saveConfigurations: Boolean? = null,
    @get:Attribute("saveInputs")
    var saveInputs: Boolean? = null,
    @get:Attribute("saveSecrets")
    var saveSecrets: Boolean? = null,
    @get:Attribute("dialogIsModal")
    var dialogIsModal: Boolean? = null,
    @get:Attribute("editorSoftWraps")
    var editorSoftWraps: Boolean? = null,
    @get:Attribute("editorShowSpecialCharacters")
    var editorShowSpecialCharacters: Boolean? = null,
    @get:Attribute("editorShowWhitespaces")
    var editorShowWhitespaces: Boolean? = null,
    @get:XCollection(style = XCollection.Style.v2, elementName = "expandedGroupNodeId")
    var expandedGroupNodeIds: List<String>? = null,
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}