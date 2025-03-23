package dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.tool.editor.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.action.EncodeDecodeActionGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.PsiJavaUtils

class EncodeDecodeJavaCodeActionGroup : EncodeDecodeActionGroup() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getSourceText(e: AnActionEvent): Pair<String, TextRange>? =
    PsiJavaUtils.getPsiElementAtCaret(e)?.let { PsiJavaUtils.getTextIfStringValueOrIdentifier(it) }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
