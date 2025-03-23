package dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.PsiJavaUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention.TextCaseConverterIntentionAction

class TextCaseConverterJavaCodeIntentionAction : TextCaseConverterIntentionAction() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getFamilyName(): String = "Convert text case of Java string or identifier"

  override fun getText(): String = "Convert text case"

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? {
    val psiElement = file.findElementAt(editor.caretModel.offset) ?: return null
    return PsiJavaUtils.getTextIfStringValueOrIdentifier(psiElement)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
