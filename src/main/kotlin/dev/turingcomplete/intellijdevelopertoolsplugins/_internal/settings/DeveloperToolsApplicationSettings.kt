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

  val loadExamples: ValueProperty<Boolean> = ValueProperty(LOAD_EXAMPLES_DEFAULT)
  val saveConfigurations: ValueProperty<Boolean> = ValueProperty(SAVE_CONFIGURATIONS_DEFAULT)
  val saveInputs: ValueProperty<Boolean> = ValueProperty(SAVE_INPUTS_DEFAULT)
  val saveSecrets: ValueProperty<Boolean> = ValueProperty(SAVE_SECRETS_DEFAULT)
  val editorSoftWraps: ValueProperty<Boolean> = ValueProperty(EDITOR_SOFT_WRAPS_DEFAULT)
  val editorShowSpecialCharacters: ValueProperty<Boolean> = ValueProperty(EDITOR_SHOW_SPECIAL_CHARACTERS_DEFAULT)
  val editorShowWhitespaces: ValueProperty<Boolean> = ValueProperty(EDITOR_SHOW_WHITESPACES_DEFAULT)
  val toolWindowMenuHideOnToolSelection: ValueProperty<Boolean> = ValueProperty(TOOL_WINDOW_MENU_HIDE_ON_TOOL_SELECTION)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getState(): ApplicationState = ApplicationState(
    loadExamples = loadExamples.get(),
    saveConfigurations = saveConfigurations.get(),
    saveInputs = saveInputs.get(),
    saveSecrets = saveSecrets.get(),
    editorSoftWraps = editorSoftWraps.get(),
    editorShowSpecialCharacters = editorShowSpecialCharacters.get(),
    editorShowWhitespaces = editorShowWhitespaces.get()
  )

  override fun loadState(state: ApplicationState) {
    loadExamples.set(state.loadExamples ?: LOAD_EXAMPLES_DEFAULT)
    saveConfigurations.set(state.saveConfigurations ?: SAVE_CONFIGURATIONS_DEFAULT)
    saveInputs.set(state.saveInputs ?: SAVE_INPUTS_DEFAULT)
    saveSecrets.set(state.saveSecrets ?: SAVE_SECRETS_DEFAULT)
    editorSoftWraps.set(state.editorSoftWraps ?: EDITOR_SOFT_WRAPS_DEFAULT)
    editorShowSpecialCharacters.set(state.editorShowSpecialCharacters ?: EDITOR_SHOW_SPECIAL_CHARACTERS_DEFAULT)
    editorShowWhitespaces.set(state.editorShowWhitespaces ?: EDITOR_SHOW_WHITESPACES_DEFAULT)
    toolWindowMenuHideOnToolSelection.set(state.toolWindowMenuHideOnClick ?: TOOL_WINDOW_MENU_HIDE_ON_TOOL_SELECTION)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class ApplicationState(
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

    const val LOAD_EXAMPLES_DEFAULT = true
    const val SAVE_INPUTS_DEFAULT = true
    const val SAVE_SECRETS_DEFAULT = true
    const val SAVE_CONFIGURATIONS_DEFAULT = true
    const val EDITOR_SOFT_WRAPS_DEFAULT = true
    const val EDITOR_SHOW_SPECIAL_CHARACTERS_DEFAULT = false
    const val EDITOR_SHOW_WHITESPACES_DEFAULT = false
    const val TOOL_WINDOW_MENU_HIDE_ON_TOOL_SELECTION = true

    var loadExamples by instance.loadExamples
    var saveConfigurations by instance.saveConfigurations
    var saveInputs by instance.saveInputs
    var saveSecrets by instance.saveSecrets
    var editorSoftWraps by instance.editorSoftWraps
    var editorShowSpecialCharacters by instance.editorShowSpecialCharacters
    var editorShowWhitespaces by instance.editorShowWhitespaces
    var toolWindowMenuHideOnToolSelection by instance.toolWindowMenuHideOnToolSelection
  }
}