package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EditorSourceText.getSelectedTextOrTextAtCaret
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle

class EncodeDecodeCodeIntentionAction : EncodeDecodeIntentionAction() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getFamilyName(): String =
    EditorToolsBundle.message("intention.encode-decode.family-name")

  override fun getText(): String = EditorToolsBundle.message("intention.encode-decode.text")

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? =
    editor.getSelectedTextOrTextAtCaret()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
