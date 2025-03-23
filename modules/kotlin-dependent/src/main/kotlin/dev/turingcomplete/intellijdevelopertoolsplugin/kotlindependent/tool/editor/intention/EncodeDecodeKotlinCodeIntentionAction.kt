package dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.PsiKotlinUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention.EncodeDecodeIntentionAction

class EncodeDecodeKotlinCodeIntentionAction : EncodeDecodeIntentionAction() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getFamilyName(): String = "Encode or decode Kotlin string or identifier"

  override fun getText(): String = "Encode or decode"

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? {
    val psiElement = file.findElementAt(editor.caretModel.offset) ?: return null
    return PsiKotlinUtils.getTextFromStringValueOrIdentifier(psiElement)?.let { it to psiElement.textRange }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
