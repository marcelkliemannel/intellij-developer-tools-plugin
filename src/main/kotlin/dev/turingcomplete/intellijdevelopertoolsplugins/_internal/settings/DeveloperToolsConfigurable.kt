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
  private lateinit var editorSoftWraps: ValueProperty<Boolean>
  private lateinit var editorShowSpecialCharacters: ValueProperty<Boolean>
  private lateinit var editorShowWhitespaces: ValueProperty<Boolean>

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getDisplayName(): String = "Developer Tools"

  override fun createComponent(): JComponent = panel {
    row {
      saveConfigurations = ValueProperty(DeveloperToolsPluginService.saveConfigurations)
      checkBox("Remember configurations")
        .bindSelected(saveConfigurations)
    }
    row {
      saveInputs = ValueProperty(DeveloperToolsPluginService.saveInputs)
      checkBox("Remember inputs")
        .bindSelected(saveInputs)
    }
    row {
      saveSecrets = ValueProperty(DeveloperToolsPluginService.saveSecrets)
      checkBox("Remember secrets")
        .bindSelected(saveSecrets)
    }.comment("Secrets are stored in the <a href='https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html#storage'>system keychain</a>.") {
      BrowserUtil.browse(it.url)
    }
    row {
      loadExamples = ValueProperty(DeveloperToolsPluginService.loadExamples)
      checkBox("Load examples")
        .bindSelected(loadExamples)
    }

    groupRowsRange("Default Editor Settings") {
      row {
        editorSoftWraps = ValueProperty(DeveloperToolsPluginService.editorSoftWraps)
        checkBox("Soft-wrap")
          .bindSelected(editorSoftWraps)
      }
      row {
        editorShowSpecialCharacters = ValueProperty(DeveloperToolsPluginService.editorShowSpecialCharacters)
        checkBox("Show special characters")
          .bindSelected(editorShowSpecialCharacters)
      }
      row {
        editorShowWhitespaces = ValueProperty(DeveloperToolsPluginService.editorShowWhitespaces)
        checkBox("Show whitespaces")
          .bindSelected(editorShowWhitespaces)
      }
    }

    groupRowsRange("Advanced") {
      row {
        dialogIsModal = ValueProperty(DeveloperToolsPluginService.dialogIsModal)
        checkBox("Dialog is modal")
          .bindSelected(dialogIsModal)
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
    DeveloperToolsPluginService.saveConfigurations != saveConfigurations.get() ||
            DeveloperToolsPluginService.saveInputs != saveInputs.get() ||
            DeveloperToolsPluginService.saveSecrets != saveSecrets.get() ||
            DeveloperToolsPluginService.loadExamples != loadExamples.get() ||
            DeveloperToolsPluginService.dialogIsModal != dialogIsModal.get() ||
            DeveloperToolsPluginService.editorSoftWraps != editorSoftWraps.get() ||
            DeveloperToolsPluginService.editorShowWhitespaces != editorShowWhitespaces.get() ||
            DeveloperToolsPluginService.editorShowSpecialCharacters != editorShowSpecialCharacters.get()

  override fun apply() {
    DeveloperToolsPluginService.saveConfigurations = saveConfigurations.get()
    DeveloperToolsPluginService.saveInputs = saveInputs.get()
    DeveloperToolsPluginService.saveSecrets = saveSecrets.get()
    DeveloperToolsPluginService.loadExamples = loadExamples.get()
    DeveloperToolsPluginService.dialogIsModal = dialogIsModal.get()
    DeveloperToolsPluginService.editorSoftWraps = editorSoftWraps.get()
    DeveloperToolsPluginService.editorShowWhitespaces = editorShowWhitespaces.get()
    DeveloperToolsPluginService.editorShowSpecialCharacters = editorShowSpecialCharacters.get()
  }

  override fun reset() {
    saveConfigurations.set(DeveloperToolsPluginService.SAVE_CONFIGURATIONS_DEFAULT)
    saveInputs.set(DeveloperToolsPluginService.SAVE_INPUTS_DEFAULT)
    saveSecrets.set(DeveloperToolsPluginService.SAVE_SECRETS_DEFAULT)
    loadExamples.set(DeveloperToolsPluginService.LOAD_EXAMPLES_DEFAULT)
    dialogIsModal.set(DeveloperToolsPluginService.DIALOG_IS_MODAL_DEFAULT)
    editorSoftWraps.set(DeveloperToolsPluginService.EDITOR_SOFT_WRAPS_DEFAULT)
    editorShowWhitespaces.set(DeveloperToolsPluginService.EDITOR_SHOW_WHITESPACES_DEFAULT)
    editorShowSpecialCharacters.set(DeveloperToolsPluginService.EDITOR_SHOW_SPECIAL_CHARACTERS_DEFAULT)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}