package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EditorSourceText.getSelectedTextOrTextAtCaret

class EncodeDecodeCodeIntentionAction : EncodeDecodeIntentionAction() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getFamilyName(): String = "Encode or decode selected text or identifier"

  override fun getText(): String = "Encode or decode"

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? =
    editor.getSelectedTextOrTextAtCaret()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
