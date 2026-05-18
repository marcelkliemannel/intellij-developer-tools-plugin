package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor

import com.intellij.codeInsight.editorActions.SelectWordUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.getSelectedText
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.getSelectedTexts

object EditorSourceText {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun Editor.getSelectedTextOrTextAtCaret(): Pair<String, TextRange>? =
    getSelectedText() ?: getTextAtCaret()

  fun Editor.getSelectedTextsOrTextAtCaret(): List<Pair<String, TextRange>> {
    val selectedTexts = getSelectedTexts()
    return selectedTexts.ifEmpty { listOfNotNull(getTextAtCaret()) }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun Editor.getTextAtCaret(): Pair<String, TextRange>? {
    val textRange =
      SelectWordUtil.getWordOrLexemeSelectionRange(
        this,
        caretModel.offset,
        { it.isLetterOrDigit() || it == '_' },
      ) ?: return null

    return document.getText(textRange) to textRange
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
