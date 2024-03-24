package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.intention

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.PsiJavaUtils

/**
 * Some code parts of this class are only available of the optional dependency
 * `com.intellij.java` is available.
 */
internal class EscapeUnescapeJavaCodeIntentionAction : EscapeUnescapeIntentionAction() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getFamilyName(): String = "Escape or unescape Java string"

  override fun getText(): String = "Escape or unescape"

  override fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>? {
    val psiElement = file.findElementAt(editor.caretModel.offset) ?: return null
    return PsiJavaUtils.getTextIfStringValue(psiElement)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}