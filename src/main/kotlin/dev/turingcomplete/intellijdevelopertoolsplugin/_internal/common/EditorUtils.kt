package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.util.TextRange

object EditorUtils {
  // -- Variables --------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun Editor.getSelectedText(): Pair<String, TextRange>? {
    val selectionModel = this.selectionModel
    val selectedTextRange = selectionModel.getTextRangeIfSelection() ?: return null
    val selectedText = selectionModel.selectedText ?: return null
    return selectedText to selectedTextRange
  }

  fun Editor.executeWriteCommand(actionName: String, action: (Editor) -> Unit) {
    CommandProcessor.getInstance().executeCommand(this.project, {
      runWriteAction {
        action(this)
      }
    }, actionName, null, this.document)
  }

  fun AnActionEvent.getEditor(): Editor =
    this.getData(CommonDataKeys.EDITOR) ?: error("Editor not found")

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun SelectionModel.getTextRangeIfSelection(): TextRange? =
    if (selectionStart < selectionEnd) {
      TextRange(selectionStart, selectionEnd)
    }
    else {
      null
    }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}