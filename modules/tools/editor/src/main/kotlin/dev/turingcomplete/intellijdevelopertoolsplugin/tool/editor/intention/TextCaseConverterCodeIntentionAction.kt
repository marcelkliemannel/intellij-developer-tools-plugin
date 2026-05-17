package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EditorSourceText.getSelectedTextOrTextAtCaret

class TextCaseConverterCodeIntentionAction : TextCaseConverterIntentionAction() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getFamilyName(): String = "Convert text case of selected text or identifier"

  override fun getText(): String = "Convert text case"

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? =
    editor.getSelectedTextOrTextAtCaret()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
