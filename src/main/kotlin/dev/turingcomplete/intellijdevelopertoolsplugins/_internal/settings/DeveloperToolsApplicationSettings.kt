package dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Attribute
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings.DeveloperToolsApplicationSettings.ApplicationState
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty

@State(
  name = "DeveloperToolsApplicationSettingsV1",
  storages = [Storage("developer-tools.xml")],
  category = SettingsCategory.TOOLS
)
internal class DeveloperToolsApplicationSettings : PersistentStateComponent<ApplicationState> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  var addOpenMainDialogActionToMainToolbar: Boolean by ValueProperty(ADD_OPEN_MAIN_DIALOG_ACTION_TO_MAIN_TOOLBAR_DEFAULT)
  var promoteAddOpenMainDialogActionToMainToolbar: Boolean by ValueProperty(PROMOTE_ADD_OPEN_MAIN_DIALOG_ACTION_TO_MAIN_TOOLBAR)
  var loadExamples: Boolean by ValueProperty(LOAD_EXAMPLES_DEFAULT)
  var saveConfigurations: Boolean by ValueProperty(SAVE_CONFIGURATIONS_DEFAULT)
  var saveInputs: Boolean by ValueProperty(SAVE_INPUTS_DEFAULT)
  var saveSecrets: Boolean by ValueProperty(SAVE_SECRETS_DEFAULT)
  var editorSoftWraps: Boolean by ValueProperty(EDITOR_SOFT_WRAPS_DEFAULT)
  var editorShowSpecialCharacters: Boolean by ValueProperty(EDITOR_SHOW_SPECIAL_CHARACTERS_DEFAULT)
  var editorShowWhitespaces: Boolean by ValueProperty(EDITOR_SHOW_WHITESPACES_DEFAULT)
  var toolWindowMenuHideOnToolSelection: Boolean by ValueProperty(TOOL_WINDOW_MENU_HIDE_ON_TOOL_SELECTION)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getState(): ApplicationState = ApplicationState(
    addOpenMainDialogActionToMainToolbar = addOpenMainDialogActionToMainToolbar,
    promoteAddOpenMainDialogActionToMainToolbar = promoteAddOpenMainDialogActionToMainToolbar,
    loadExamples = loadExamples,
    saveConfigurations = saveConfigurations,
    saveInputs = saveInputs,
    saveSecrets = saveSecrets,
    editorSoftWraps = editorSoftWraps,
    editorShowSpecialCharacters = editorShowSpecialCharacters,
    editorShowWhitespaces = editorShowWhitespaces
  )

  override fun loadState(state: ApplicationState) {
    addOpenMainDialogActionToMainToolbar = (state.addOpenMainDialogActionToMainToolbar ?: ADD_OPEN_MAIN_DIALOG_ACTION_TO_MAIN_TOOLBAR_DEFAULT)
    promoteAddOpenMainDialogActionToMainToolbar = (state.promoteAddOpenMainDialogActionToMainToolbar ?: PROMOTE_ADD_OPEN_MAIN_DIALOG_ACTION_TO_MAIN_TOOLBAR)
    loadExamples = (state.loadExamples ?: LOAD_EXAMPLES_DEFAULT)
    saveConfigurations = (state.saveConfigurations ?: SAVE_CONFIGURATIONS_DEFAULT)
    saveInputs = (state.saveInputs ?: SAVE_INPUTS_DEFAULT)
    saveSecrets = (state.saveSecrets ?: SAVE_SECRETS_DEFAULT)
    editorSoftWraps = (state.editorSoftWraps ?: EDITOR_SOFT_WRAPS_DEFAULT)
    editorShowSpecialCharacters = (state.editorShowSpecialCharacters ?: EDITOR_SHOW_SPECIAL_CHARACTERS_DEFAULT)
    editorShowWhitespaces = (state.editorShowWhitespaces ?: EDITOR_SHOW_WHITESPACES_DEFAULT)
    toolWindowMenuHideOnToolSelection = (state.toolWindowMenuHideOnClick ?: TOOL_WINDOW_MENU_HIDE_ON_TOOL_SELECTION)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class ApplicationState(
    @get:Attribute("addOpenMainDialogActionToMainToolbar")
    var addOpenMainDialogActionToMainToolbar: Boolean? = null,
    @get:Attribute("promoteAddOpenMainDialogActionToMainToolbar")
    var promoteAddOpenMainDialogActionToMainToolbar: Boolean? = null,
    @get:Attribute("loadExamples")
    var loadExamples: Boolean? = null,
    @get:Attribute("saveConfigurations")
    var saveConfigurations: Boolean? = null,
    @get:Attribute("saveInputs")
    var saveInputs: Boolean? = null,
    @get:Attribute("saveSecrets")
    var saveSecrets: Boolean? = null,
    @get:Attribute("editorSoftWraps")
    var editorSoftWraps: Boolean? = null,
    @get:Attribute("editorShowSpecialCharacters")
    var editorShowSpecialCharacters: Boolean? = null,
    @get:Attribute("editorShowWhitespaces")
    var editorShowWhitespaces: Boolean? = null,
    @get:Attribute("toolWindowMenuHideOnClick")
    var toolWindowMenuHideOnClick: Boolean? = null
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val instance: DeveloperToolsApplicationSettings
      get() = ApplicationManager.getApplication().getService(DeveloperToolsApplicationSettings::class.java)

    const val ADD_OPEN_MAIN_DIALOG_ACTION_TO_MAIN_TOOLBAR_DEFAULT = false
    const val PROMOTE_ADD_OPEN_MAIN_DIALOG_ACTION_TO_MAIN_TOOLBAR = true
    const val LOAD_EXAMPLES_DEFAULT = true
    const val SAVE_INPUTS_DEFAULT = true
    const val SAVE_SECRETS_DEFAULT = true
    const val SAVE_CONFIGURATIONS_DEFAULT = true
    const val EDITOR_SOFT_WRAPS_DEFAULT = true
    const val EDITOR_SHOW_SPECIAL_CHARACTERS_DEFAULT = false
    const val EDITOR_SHOW_WHITESPACES_DEFAULT = false
    const val TOOL_WINDOW_MENU_HIDE_ON_TOOL_SELECTION = true
  }
}