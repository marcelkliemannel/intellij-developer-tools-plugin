package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.PlatformIcons
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.CommonsDataKeys.SELECTED_VALUES
import java.awt.datatransfer.StringSelection

class CopyValuesAction : DumbAwareAction("Copy Values", null, PlatformIcons.COPY_ICON) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    val selectedValues = SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")

    e.presentation.isVisible = selectedValues.isNotEmpty()
    e.presentation.text = if (selectedValues.size > 1) {
      "Copy ${selectedValues.size} Values"
    }
    else {
      "Copy Value"
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val selectedProperties: List<String> = SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")
    if (selectedProperties.isEmpty()) {
      return
    }

    CopyPasteManager.getInstance().setContents(StringSelection(selectedProperties.joinToString(System.lineSeparator())))
  }

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}