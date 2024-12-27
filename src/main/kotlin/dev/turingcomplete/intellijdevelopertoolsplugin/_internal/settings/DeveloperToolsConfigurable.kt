package dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.UiUtils
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.not
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings.ActionHandlingInstance
import dev.turingcomplete.intellijdevelopertoolsplugin.i18n.I18nUtils
import javax.swing.JComponent

class DeveloperToolsConfigurable : Configurable {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var addOpenMainDialogActionToMainToolbar: ValueProperty<Boolean>
  private lateinit var saveConfigurations: ValueProperty<Boolean>
  private lateinit var saveInputs: ValueProperty<Boolean>
  private lateinit var saveSensitiveInputs: ValueProperty<Boolean>
  private lateinit var loadExamples: ValueProperty<Boolean>
  private lateinit var dialogIsModal: ValueProperty<Boolean>
  private lateinit var editorSoftWraps: ValueProperty<Boolean>
  private lateinit var editorShowSpecialCharacters: ValueProperty<Boolean>
  private lateinit var editorShowWhitespaces: ValueProperty<Boolean>
  private lateinit var toolsMenuShowGroupNodes: ValueProperty<Boolean>
  private lateinit var toolsMenuOrderAlphabetically: ValueProperty<Boolean>
  private lateinit var autoDetectActionHandlingInstance: ValueProperty<Boolean>
  private lateinit var selectedActionHandlingInstance: ValueProperty<ActionHandlingInstance>
  private lateinit var showInternalTools: ValueProperty<Boolean>
  private lateinit var hideWorkbenchTabsOnSingleTab: ValueProperty<Boolean>

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getDisplayName(): String = "Developer Tools"

  override fun createComponent(): JComponent = panel {
    val developerToolsApplicationSettings = DeveloperToolsApplicationSettings.instance

    row {
      addOpenMainDialogActionToMainToolbar = ValueProperty(developerToolsApplicationSettings.addOpenMainDialogActionToMainToolbar)
      checkBox(I18nUtils.message("DeveloperToolsConfigurable.add_to_main_toolbar"))
        .comment(I18nUtils.message("DeveloperToolsConfigurable.add_to_main_toolbar.desc"))
        .bindSelected(addOpenMainDialogActionToMainToolbar)
    }

    row {
      saveConfigurations = ValueProperty(developerToolsApplicationSettings.saveConfigurations)
      checkBox(I18nUtils.message("DeveloperToolsConfigurable.remember_configurations"))
        .bindSelected(saveConfigurations)
    }
    row {
      saveInputs = ValueProperty(developerToolsApplicationSettings.saveInputs)
      checkBox(I18nUtils.message("DeveloperToolsConfigurable.remember_inputs"))
        .bindSelected(saveInputs)
    }
    row {
      saveSensitiveInputs = ValueProperty(developerToolsApplicationSettings.saveSensitiveInputs)
      checkBox(I18nUtils.message("DeveloperToolsConfigurable.remember_sensitive_inputs"))
        .bindSelected(saveSensitiveInputs)
    }.comment(I18nUtils.message("DeveloperToolsConfigurable.remember_sensitive.desc"))
    row {
      loadExamples = ValueProperty(developerToolsApplicationSettings.loadExamples)
      checkBox(I18nUtils.message("DeveloperToolsConfigurable.load_examples"))
        .bindSelected(loadExamples)
    }.bottomGap(BottomGap.SMALL)
    row {
      hideWorkbenchTabsOnSingleTab = ValueProperty(developerToolsApplicationSettings.hideWorkbenchTabsOnSingleTab)
      checkBox(I18nUtils.message("DeveloperToolsConfigurable.hide_workbench"))
        .bindSelected(hideWorkbenchTabsOnSingleTab)
    }.bottomGap(BottomGap.SMALL)

    buttonsGroup(I18nUtils.message("DeveloperToolsConfigurable.external")) {
      row(I18nUtils.message("DeveloperToolsConfigurable.external.open")) {
        autoDetectActionHandlingInstance = ValueProperty(developerToolsApplicationSettings.autoDetectActionHandlingInstance)
        radioButton(I18nUtils.message("DeveloperToolsConfigurable.external.auto"))
          .bindSelected(autoDetectActionHandlingInstance)

        radioButton(I18nUtils.message("DeveloperToolsConfigurable.external.use"))
          .bindSelected(autoDetectActionHandlingInstance.not())
          .gap(RightGap.SMALL)
        selectedActionHandlingInstance = ValueProperty(developerToolsApplicationSettings.selectedActionHandlingInstance)
        comboBox(ActionHandlingInstance.entries)
          .bindItem(selectedActionHandlingInstance)
          .enabledIf(autoDetectActionHandlingInstance.not())
      }.bottomGap(BottomGap.NONE)
      row {
        comment(I18nUtils.message("DeveloperToolsConfigurable.external.desc"))
      }
    }

    groupRowsRange(I18nUtils.message("DeveloperToolsConfigurable.editor_settings")) {
      row {
        editorSoftWraps = ValueProperty(developerToolsApplicationSettings.editorSoftWraps)
        checkBox(I18nUtils.message("editor.soft_wrap"))
          .bindSelected(editorSoftWraps)
      }
      row {
        editorShowSpecialCharacters = ValueProperty(developerToolsApplicationSettings.editorShowSpecialCharacters)
        checkBox(I18nUtils.message("editor.show_special_characters"))
          .bindSelected(editorShowSpecialCharacters)
      }
      row {
        editorShowWhitespaces = ValueProperty(developerToolsApplicationSettings.editorShowWhitespaces)
        checkBox(I18nUtils.message("editor.show_whitespaces"))
          .bindSelected(editorShowWhitespaces)
      }
    }

    groupRowsRange(I18nUtils.message("DeveloperToolsConfigurable.advanced")) {
      row {
        dialogIsModal = ValueProperty(DeveloperToolsDialogSettings.instance.dialogIsModal)
        checkBox(I18nUtils.message("DeveloperToolsConfigurable.advanced.1"))
          .bindSelected(dialogIsModal)
      }
      row {
        toolsMenuShowGroupNodes = ValueProperty(developerToolsApplicationSettings.toolsMenuTreeShowGroupNodes)
        checkBox(I18nUtils.message("DeveloperToolsConfigurable.advanced.2"))
          .bindSelected(toolsMenuShowGroupNodes)
      }
      row {
        toolsMenuOrderAlphabetically = ValueProperty(developerToolsApplicationSettings.toolsMenuTreeOrderAlphabetically)
        checkBox(I18nUtils.message("DeveloperToolsConfigurable.advanced.3"))
          .bindSelected(toolsMenuOrderAlphabetically)
      }
      row {
        showInternalTools = ValueProperty(developerToolsApplicationSettings.showInternalTools)
        checkBox(I18nUtils.message("DeveloperToolsConfigurable.advanced.4"))
          .bindSelected(showInternalTools)
          .comment(I18nUtils.message("DeveloperToolsConfigurable.advanced.4.desc"))
      }
    }

    row {
      cell(
        UiUtils.createLink(
          title = I18nUtils.message("DeveloperToolsConfigurable.issue"),
          url = "https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues"
        )
      )
    }.topGap(TopGap.MEDIUM)
  }

  override fun isModified(): Boolean {
    val developerToolsApplicationSettings = DeveloperToolsApplicationSettings.instance
    return developerToolsApplicationSettings.addOpenMainDialogActionToMainToolbar != addOpenMainDialogActionToMainToolbar.get() ||
            developerToolsApplicationSettings.saveConfigurations != saveConfigurations.get() ||
            developerToolsApplicationSettings.saveInputs != saveInputs.get() ||
            developerToolsApplicationSettings.saveSensitiveInputs != saveSensitiveInputs.get() ||
            developerToolsApplicationSettings.loadExamples != loadExamples.get() ||
            DeveloperToolsDialogSettings.instance.dialogIsModal != dialogIsModal.get() ||
            developerToolsApplicationSettings.editorSoftWraps != editorSoftWraps.get() ||
            developerToolsApplicationSettings.editorShowWhitespaces != editorShowWhitespaces.get() ||
            developerToolsApplicationSettings.editorShowSpecialCharacters != editorShowSpecialCharacters.get() ||
            developerToolsApplicationSettings.toolsMenuTreeShowGroupNodes != toolsMenuShowGroupNodes.get() ||
            developerToolsApplicationSettings.toolsMenuTreeOrderAlphabetically != toolsMenuOrderAlphabetically.get() ||
            developerToolsApplicationSettings.autoDetectActionHandlingInstance != autoDetectActionHandlingInstance.get() ||
            developerToolsApplicationSettings.selectedActionHandlingInstance != selectedActionHandlingInstance.get() ||
            developerToolsApplicationSettings.showInternalTools != showInternalTools.get() ||
            developerToolsApplicationSettings.hideWorkbenchTabsOnSingleTab != hideWorkbenchTabsOnSingleTab.get()
  }

  override fun apply() {
    DeveloperToolsApplicationSettings.instance.update {
      it.addOpenMainDialogActionToMainToolbar = addOpenMainDialogActionToMainToolbar.get()
      it.saveConfigurations = saveConfigurations.get()
      it.saveInputs = saveInputs.get()
      it.saveSensitiveInputs = saveSensitiveInputs.get()
      it.loadExamples = loadExamples.get()
      it.editorSoftWraps = editorSoftWraps.get()
      it.editorShowWhitespaces = editorShowWhitespaces.get()
      it.editorShowSpecialCharacters = editorShowSpecialCharacters.get()
      it.toolsMenuTreeShowGroupNodes = toolsMenuShowGroupNodes.get()
      it.toolsMenuTreeOrderAlphabetically = toolsMenuOrderAlphabetically.get()
      it.autoDetectActionHandlingInstance = autoDetectActionHandlingInstance.get()
      it.selectedActionHandlingInstance = selectedActionHandlingInstance.get()
      it.showInternalTools = showInternalTools.get()
      it.hideWorkbenchTabsOnSingleTab = hideWorkbenchTabsOnSingleTab.get()
    }
    DeveloperToolsDialogSettings.instance.dialogIsModal = dialogIsModal.get()
  }

  override fun reset() {
    val developerToolsApplicationSettings = DeveloperToolsApplicationSettings.instance
    addOpenMainDialogActionToMainToolbar.set(developerToolsApplicationSettings.addOpenMainDialogActionToMainToolbar)
    saveConfigurations.set(developerToolsApplicationSettings.saveConfigurations)
    saveInputs.set(developerToolsApplicationSettings.saveInputs)
    saveSensitiveInputs.set(developerToolsApplicationSettings.saveSensitiveInputs)
    loadExamples.set(developerToolsApplicationSettings.loadExamples)
    dialogIsModal.set(DeveloperToolsDialogSettings.instance.dialogIsModal)
    editorSoftWraps.set(developerToolsApplicationSettings.editorSoftWraps)
    editorShowWhitespaces.set(developerToolsApplicationSettings.editorShowWhitespaces)
    editorShowSpecialCharacters.set(developerToolsApplicationSettings.editorShowSpecialCharacters)
    toolsMenuShowGroupNodes.set(developerToolsApplicationSettings.toolsMenuTreeShowGroupNodes)
    toolsMenuOrderAlphabetically.set(developerToolsApplicationSettings.toolsMenuTreeOrderAlphabetically)
    autoDetectActionHandlingInstance.set(developerToolsApplicationSettings.autoDetectActionHandlingInstance)
    selectedActionHandlingInstance.set(developerToolsApplicationSettings.selectedActionHandlingInstance)
    showInternalTools.set(developerToolsApplicationSettings.showInternalTools)
    hideWorkbenchTabsOnSingleTab.set(developerToolsApplicationSettings.hideWorkbenchTabsOnSingleTab)
    apply()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}