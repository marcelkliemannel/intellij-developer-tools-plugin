package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.PsiKotlinUtils

/**
 * Some code parts of this class are only available of the optional dependency
 * `org.jetbrains.kotlin` is available.
 */
internal class EscapeUnescapeKotlinCodeIntentionAction : EncodeDecodeIntentionAction() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getFamilyName(): String = "Escape or unescape Kotlin string"

  override fun getText(): String = "Escape or unescape"

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? {
    val psiElement = file.findElementAt(editor.caretModel.offset) ?: return null
    return PsiKotlinUtils.getTextFromStringValue(psiElement)?.let { it to psiElement.textRange }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}