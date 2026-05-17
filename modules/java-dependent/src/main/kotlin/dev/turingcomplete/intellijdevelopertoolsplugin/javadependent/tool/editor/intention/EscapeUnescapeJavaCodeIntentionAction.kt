package dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.PsiJavaUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.message.JavaDependentBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention.EscapeUnescapeIntentionAction

class EscapeUnescapeJavaCodeIntentionAction : EscapeUnescapeIntentionAction() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getFamilyName(): String =
    JavaDependentBundle.message("intention.escape-unescape-java-code.family-name")

  override fun getText(): String =
    JavaDependentBundle.message("intention.escape-unescape-java-code.text")

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? {
    val psiElement = file.findElementAt(editor.caretModel.offset) ?: return null
    return PsiJavaUtils.getTextIfStringValue(psiElement)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
