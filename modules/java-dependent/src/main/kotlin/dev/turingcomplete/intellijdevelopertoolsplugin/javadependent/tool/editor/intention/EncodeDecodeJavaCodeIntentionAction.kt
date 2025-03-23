package dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.PsiJavaUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention.EncodeDecodeIntentionAction

class EncodeDecodeJavaCodeIntentionAction : EncodeDecodeIntentionAction() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getFamilyName(): String = "Encode or decode Java string or identifier"

  override fun getText(): String = "Encode or decode"

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? {
    val psiElement = file.findElementAt(editor.caretModel.offset) ?: return null
    return PsiJavaUtils.getTextIfStringValueOrIdentifier(psiElement)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
