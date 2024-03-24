package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.PsiJavaUtils

/**
 * Some code parts of this class are only available of the optional dependency
 * `com.intellij.java` is available.
 */
internal class EncodeDecodeJavaCodeActionGroup : EncodeDecodeActionGroup() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getSourceText(e: AnActionEvent): Pair<String, TextRange>? =
    PsiJavaUtils.getPsiElementAtCaret(e)?.let { PsiJavaUtils.getTextIfStringValueOrIdentifier(it) }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}