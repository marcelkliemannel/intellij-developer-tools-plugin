package dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings

import com.intellij.ide.BrowserUtil
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
import javax.swing.JComponent

class DeveloperToolsConfigurable : Configurable {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var addOpenMainDialogActionToMainToolbar: ValueProperty<Boolean>
  private lateinit var saveConfigurations: ValueProperty<Boolean>
  private lateinit var saveInputs: ValueProperty<Boolean>
  private lateinit var saveSecrets: ValueProperty<Boolean>
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
      checkBox("Add 'Developer Tools' action to the main toolbar during startup")
        .comment("If the action was removed manually in the past, this automatic mechanism will not work. The 'Developer Tools' action must first be manually added to the 'Main Toolbar Right' again (or 'Main Toolbar' in the old UI).")
        .bindSelected(addOpenMainDialogActionToMainToolbar)
    }

    row {
      saveConfigurations = ValueProperty(developerToolsApplicationSettings.saveConfigurations)
      checkBox("Remember configurations")
        .bindSelected(saveConfigurations)
    }
    row {
      saveInputs = ValueProperty(developerToolsApplicationSettings.saveInputs)
      checkBox("Remember inputs")
        .bindSelected(saveInputs)
    }
    row {
      saveSecrets = ValueProperty(developerToolsApplicationSettings.saveSecrets)
      checkBox("Remember secrets")
        .bindSelected(saveSecrets)
    }.comment("Secrets are stored in the <a href='https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html#storage'>system keychain</a>.") {
      BrowserUtil.browse(it.url)
    }
    row {
      loadExamples = ValueProperty(developerToolsApplicationSettings.loadExamples)
      checkBox("Load examples")
        .bindSelected(loadExamples)
    }.bottomGap(BottomGap.SMALL)
    row {
      hideWorkbenchTabsOnSingleTab = ValueProperty(developerToolsApplicationSettings.hideWorkbenchTabsOnSingleTab)
      checkBox("Hide workbench tabs if there is only one tab")
        .bindSelected(hideWorkbenchTabsOnSingleTab)
    }.bottomGap(BottomGap.SMALL)

    buttonsGroup("External Action Handling") {
      row("Open tools in:") {
        autoDetectActionHandlingInstance = ValueProperty(developerToolsApplicationSettings.autoDetectActionHandlingInstance)
        radioButton("Auto detect")
          .bindSelected(autoDetectActionHandlingInstance)

        radioButton("Use:")
          .bindSelected(autoDetectActionHandlingInstance.not())
          .gap(RightGap.SMALL)
        selectedActionHandlingInstance = ValueProperty(developerToolsApplicationSettings.selectedActionHandlingInstance)
        comboBox(ActionHandlingInstance.entries)
          .bindItem(selectedActionHandlingInstance)
          .enabledIf(autoDetectActionHandlingInstance.not())
      }.bottomGap(BottomGap.NONE)
      row {
        comment("The plugin provides actions in other places in IntelliJ (e.g. in the editor or the project file tree) that open a developer tool. With this setting you can specify whether these actions open the dialog or the tool window.")
      }
    }

    groupRowsRange("Default Editor Settings") {
      row {
        editorSoftWraps = ValueProperty(developerToolsApplicationSettings.editorSoftWraps)
        checkBox("Soft-wrap")
          .bindSelected(editorSoftWraps)
      }
      row {
        editorShowSpecialCharacters = ValueProperty(developerToolsApplicationSettings.editorShowSpecialCharacters)
        checkBox("Show special characters")
          .bindSelected(editorShowSpecialCharacters)
      }
      row {
        editorShowWhitespaces = ValueProperty(developerToolsApplicationSettings.editorShowWhitespaces)
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
        toolsMenuShowGroupNodes = ValueProperty(developerToolsApplicationSettings.toolsMenuTreeShowGroupNodes)
        checkBox("Group tools in the menu")
          .bindSelected(toolsMenuShowGroupNodes)
      }
      row {
        toolsMenuOrderAlphabetically = ValueProperty(developerToolsApplicationSettings.toolsMenuTreeOrderAlphabetically)
        checkBox("Order tools in the menu alphabetically")
          .bindSelected(toolsMenuOrderAlphabetically)
      }
      row {
        showInternalTools = ValueProperty(developerToolsApplicationSettings.showInternalTools)
        checkBox("Show internal tools")
          .bindSelected(showInternalTools)
          .comment("Enables additional developer tools that are only useful for special applications, such as supporting IntelliJ plugin development.")
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

  override fun isModified(): Boolean {
    val developerToolsApplicationSettings = DeveloperToolsApplicationSettings.instance
    return developerToolsApplicationSettings.addOpenMainDialogActionToMainToolbar != addOpenMainDialogActionToMainToolbar.get() ||
            developerToolsApplicationSettings.saveConfigurations != saveConfigurations.get() ||
            developerToolsApplicationSettings.saveInputs != saveInputs.get() ||
            developerToolsApplicationSettings.saveSecrets != saveSecrets.get() ||
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
      it.saveSecrets = saveSecrets.get()
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
    saveSecrets.set(developerToolsApplicationSettings.saveSecrets)
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