package dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.PsiKotlinUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.message.KotlinDependentBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention.EscapeUnescapeIntentionAction

class EscapeUnescapeKotlinCodeIntentionAction : EscapeUnescapeIntentionAction() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getFamilyName(): String =
    KotlinDependentBundle.message("intention.escape-unescape-kotlin-code.family-name")

  override fun getText(): String =
    KotlinDependentBundle.message("intention.escape-unescape-kotlin-code.text")

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? {
    val psiElement = file.findElementAt(editor.caretModel.offset) ?: return null
    return PsiKotlinUtils.getTextFromStringValue(psiElement)?.let { it to psiElement.textRange }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
