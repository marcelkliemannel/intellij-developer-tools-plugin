package dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.UiUtils
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty
import javax.swing.JComponent

class DeveloperToolsConfigurable : Configurable {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var saveConfigurations: ValueProperty<Boolean>
  private lateinit var saveInputs: ValueProperty<Boolean>
  private lateinit var saveSecrets: ValueProperty<Boolean>
  private lateinit var loadExamples: ValueProperty<Boolean>
  private lateinit var dialogIsModal: ValueProperty<Boolean>
  private lateinit var toolWindowMenuHideOnToolSelection: ValueProperty<Boolean>
  private lateinit var editorSoftWraps: ValueProperty<Boolean>
  private lateinit var editorShowSpecialCharacters: ValueProperty<Boolean>
  private lateinit var editorShowWhitespaces: ValueProperty<Boolean>

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getDisplayName(): String = "Developer Tools"

  override fun createComponent(): JComponent = panel {
    row {
      saveConfigurations = ValueProperty(DeveloperToolsApplicationSettings.instance.saveConfigurations)
      checkBox("Remember configurations")
        .bindSelected(saveConfigurations)
    }
    row {
      saveInputs = ValueProperty(DeveloperToolsApplicationSettings.instance.saveInputs)
      checkBox("Remember inputs")
        .bindSelected(saveInputs)
    }
    row {
      saveSecrets = ValueProperty(DeveloperToolsApplicationSettings.instance.saveSecrets)
      checkBox("Remember secrets")
        .bindSelected(saveSecrets)
    }.comment("Secrets are stored in the <a href='https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html#storage'>system keychain</a>.") {
      BrowserUtil.browse(it.url)
    }
    row {
      loadExamples = ValueProperty(DeveloperToolsApplicationSettings.instance.loadExamples)
      checkBox("Load examples")
        .bindSelected(loadExamples)
    }

    groupRowsRange("Default Editor Settings") {
      row {
        editorSoftWraps = ValueProperty(DeveloperToolsApplicationSettings.instance.editorSoftWraps)
        checkBox("Soft-wrap")
          .bindSelected(editorSoftWraps)
      }
      row {
        editorShowSpecialCharacters = ValueProperty(DeveloperToolsApplicationSettings.instance.editorShowSpecialCharacters)
        checkBox("Show special characters")
          .bindSelected(editorShowSpecialCharacters)
      }
      row {
        editorShowWhitespaces = ValueProperty(DeveloperToolsApplicationSettings.instance.editorShowWhitespaces)
        checkBox("Show whitespaces")
          .bindSelected(editorShowWhitespaces)
      }
    }

    groupRowsRange("Advanced") {
      row {
        dialogIsModal = ValueProperty(DeveloperToolsDialogSettings.instance.dialogIsModal)
        checkBox("Dialog is modal and must be closed before continuing to work with IntelliJ")
          .bindSelected(dialogIsModal)
      }
      row {
        toolWindowMenuHideOnToolSelection = ValueProperty(DeveloperToolsApplicationSettings.instance.toolWindowMenuHideOnToolSelection)
        checkBox("Hide the menu in the tool window after selecting a tool")
          .bindSelected(toolWindowMenuHideOnToolSelection)
      }
    }

    row {
      cell(
        UiUtils.createLink(
          title = "Make a feature request or report an issue",
          url = "https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues"
        )
      )
    }.topGap(TopGap.MEDIUM)
  }

  override fun isModified(): Boolean =
    DeveloperToolsApplicationSettings.instance.saveConfigurations != saveConfigurations.get() ||
            DeveloperToolsApplicationSettings.instance.saveInputs != saveInputs.get() ||
            DeveloperToolsApplicationSettings.instance.saveSecrets != saveSecrets.get() ||
            DeveloperToolsApplicationSettings.instance.loadExamples != loadExamples.get() ||
            DeveloperToolsDialogSettings.instance.dialogIsModal != dialogIsModal.get() ||
            DeveloperToolsApplicationSettings.instance.editorSoftWraps != editorSoftWraps.get() ||
            DeveloperToolsApplicationSettings.instance.editorShowWhitespaces != editorShowWhitespaces.get() ||
            DeveloperToolsApplicationSettings.instance.editorShowSpecialCharacters != editorShowSpecialCharacters.get() ||
            DeveloperToolsApplicationSettings.instance.toolWindowMenuHideOnToolSelection != toolWindowMenuHideOnToolSelection.get()

  override fun apply() {
    DeveloperToolsApplicationSettings.instance.saveConfigurations = saveConfigurations.get()
    DeveloperToolsApplicationSettings.instance.saveInputs = saveInputs.get()
    DeveloperToolsApplicationSettings.instance.saveSecrets = saveSecrets.get()
    DeveloperToolsApplicationSettings.instance.loadExamples = loadExamples.get()
    DeveloperToolsDialogSettings.instance.dialogIsModal = dialogIsModal.get()
    DeveloperToolsApplicationSettings.instance.editorSoftWraps = editorSoftWraps.get()
    DeveloperToolsApplicationSettings.instance.editorShowWhitespaces = editorShowWhitespaces.get()
    DeveloperToolsApplicationSettings.instance.editorShowSpecialCharacters = editorShowSpecialCharacters.get()
    DeveloperToolsApplicationSettings.instance.toolWindowMenuHideOnToolSelection = toolWindowMenuHideOnToolSelection.get()
  }

  override fun reset() {
    saveConfigurations.set(DeveloperToolsApplicationSettings.SAVE_CONFIGURATIONS_DEFAULT)
    saveInputs.set(DeveloperToolsApplicationSettings.SAVE_INPUTS_DEFAULT)
    saveSecrets.set(DeveloperToolsApplicationSettings.SAVE_SECRETS_DEFAULT)
    loadExamples.set(DeveloperToolsApplicationSettings.LOAD_EXAMPLES_DEFAULT)
    dialogIsModal.set(DeveloperToolsDialogSettings.DIALOG_IS_MODAL_DEFAULT)
    editorSoftWraps.set(DeveloperToolsApplicationSettings.EDITOR_SOFT_WRAPS_DEFAULT)
    editorShowWhitespaces.set(DeveloperToolsApplicationSettings.EDITOR_SHOW_WHITESPACES_DEFAULT)
    editorShowSpecialCharacters.set(DeveloperToolsApplicationSettings.EDITOR_SHOW_SPECIAL_CHARACTERS_DEFAULT)
    toolWindowMenuHideOnToolSelection.set(DeveloperToolsApplicationSettings.TOOL_WINDOW_MENU_HIDE_ON_TOOL_SELECTION)
    apply()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}