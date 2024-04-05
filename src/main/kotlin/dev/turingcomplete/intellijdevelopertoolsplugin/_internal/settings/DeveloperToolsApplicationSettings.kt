package dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.xmlb.annotations.Attribute
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings.ApplicationState
import java.security.Provider
import java.security.Security

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
  var toolsMenuTreeShowGroupNodes: Boolean by ValueProperty(TOOLS_MENU_TREE_GROUP_NODES_DEFAULT)
  var toolsMenuTreeOrderAlphabetically: Boolean by ValueProperty(TOOLS_MENU_TREE_ORDER_ALPHABETICALLY_DEFAULT)
  var autoDetectActionHandlingInstance: Boolean by ValueProperty(AUTO_DETECT_ACTION_HANDLING_INSTANCE_DEFAULT)
  var selectedActionHandlingInstance: ActionHandlingInstance by ValueProperty(SELECTED_ACTION_HANDLING_INSTANCE_DEFAULT)
  var showInternalTools: Boolean by ValueProperty(SHOW_INTERNAL_TOOLS_DEFAULT)
  var hideWorkbenchTabsOnSingleTab: Boolean by ValueProperty(SHOW_INTERNAL_TOOLS_DEFAULT)

  var modificationCounter = 0
    private set

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    try {
      val bouncyCastleProviderClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
      val bouncyCastleProvider = bouncyCastleProviderClass.getConstructor().newInstance()
      Security.addProvider(bouncyCastleProvider as Provider)
    } catch (e: Exception) {
      log.debug("Can't load BouncyCastleProvider", e)
    }
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun update(applyModifications: (DeveloperToolsApplicationSettings) -> Unit) {
    applyModifications(this)
    modificationCounter++
  }

  override fun getState(): ApplicationState = ApplicationState(
    addOpenMainDialogActionToMainToolbar = addOpenMainDialogActionToMainToolbar,
    promoteAddOpenMainDialogActionToMainToolbar = promoteAddOpenMainDialogActionToMainToolbar,
    loadExamples = loadExamples,
    saveConfigurations = saveConfigurations,
    saveInputs = saveInputs,
    saveSecrets = saveSecrets,
    editorSoftWraps = editorSoftWraps,
    editorShowSpecialCharacters = editorShowSpecialCharacters,
    editorShowWhitespaces = editorShowWhitespaces,
    toolsMenuTreeOrderAlphabetically = toolsMenuTreeOrderAlphabetically,
    toolsMenuTreeShowGroupNodes = toolsMenuTreeShowGroupNodes,
    autoDetectActionHandlingInstance = autoDetectActionHandlingInstance,
    selectedActionHandlingInstance = selectedActionHandlingInstance,
    showInternalTools = showInternalTools,
    hideWorkbenchTabsOnSingleTab = hideWorkbenchTabsOnSingleTab
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
    toolsMenuTreeShowGroupNodes = (state.toolsMenuTreeShowGroupNodes ?: TOOLS_MENU_TREE_GROUP_NODES_DEFAULT)
    toolsMenuTreeOrderAlphabetically = (state.toolsMenuTreeOrderAlphabetically ?: TOOLS_MENU_TREE_ORDER_ALPHABETICALLY_DEFAULT)
    autoDetectActionHandlingInstance = (state.autoDetectActionHandlingInstance ?: AUTO_DETECT_ACTION_HANDLING_INSTANCE_DEFAULT)
    selectedActionHandlingInstance = (state.selectedActionHandlingInstance ?: SELECTED_ACTION_HANDLING_INSTANCE_DEFAULT)
    showInternalTools = (state.showInternalTools ?: SHOW_INTERNAL_TOOLS_DEFAULT)
    hideWorkbenchTabsOnSingleTab = (state.hideWorkbenchTabsOnSingleTab ?: HIDE_WORKBENCH_TABS_ON_SINGLE_TAB)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class ActionHandlingInstance(private val title: String) {

    TOOL_WINDOW("Tool Window"),
    DIALOG("Dialog");

    override fun toString(): String = title
  }

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
    @get:Attribute("toolsMenuTreeShowGroupNodes")
    var toolsMenuTreeShowGroupNodes: Boolean? = null,
    @get:Attribute("toolsMenuTreeOrderAlphabetically")
    var toolsMenuTreeOrderAlphabetically: Boolean? = null,
    @get:Attribute("autoDetectActionHandlingInstance")
    var autoDetectActionHandlingInstance: Boolean? = null,
    @get:Attribute("selectedActionHandlingInstance")
    var selectedActionHandlingInstance: ActionHandlingInstance? = null,
    @get:Attribute("showInternalTools")
    var showInternalTools: Boolean? = null,
    @get:Attribute("hideWorkbenchTabsOnSingleTab")
    var hideWorkbenchTabsOnSingleTab: Boolean? = null
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val log = logger<DeveloperToolsInstanceSettings>()

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
    const val TOOLS_MENU_TREE_GROUP_NODES_DEFAULT = false
    const val TOOLS_MENU_TREE_ORDER_ALPHABETICALLY_DEFAULT = true
    const val AUTO_DETECT_ACTION_HANDLING_INSTANCE_DEFAULT = true
    const val SHOW_INTERNAL_TOOLS_DEFAULT = false
    val SELECTED_ACTION_HANDLING_INSTANCE_DEFAULT = ActionHandlingInstance.TOOL_WINDOW
    const val HIDE_WORKBENCH_TABS_ON_SINGLE_TAB = true
  }
}