package dev.turingcomplete.intellijdevelopertoolsplugin.common

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

object EditorUtils {
  // -- Variables ----------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun Editor.getSelectedText(): Pair<String, TextRange>? {
    return getSelectedTexts().firstOrNull()
  }

  fun Editor.getSelectedTexts(): List<Pair<String, TextRange>> {
    return caretModel.allCarets.mapNotNull { caret ->
      if (!caret.hasSelection()) {
        return@mapNotNull null
      }

      val selectedText = caret.selectedText ?: return@mapNotNull null
      selectedText to TextRange(caret.selectionStart, caret.selectionEnd)
    }
  }

  fun Editor.executeWriteCommand(actionName: String, action: (Editor) -> Unit) {
    CommandProcessor.getInstance()
      .executeCommand(
        this.project,
        { runWriteAction { action(this) } },
        actionName,
        null,
        this.document,
      )
  }

  fun AnActionEvent.getEditor(): Editor =
    this.getData(CommonDataKeys.EDITOR) ?: error("Editor not found")

  // -- Private Methods ----------------------------------------------------- //

  // -- Inner Type ---------------------------------------------------------- //
}
