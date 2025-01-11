package dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.tool.editor.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.tool.editor.action.EscapeUnescapeActionGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.PsiKotlinUtils

internal class EscapeUnescapeKotlinCodeActionGroup : EscapeUnescapeActionGroup() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getSourceText(e: AnActionEvent): Pair<String, TextRange>? =
    PsiKotlinUtils.getTextFromStringValue(e)

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}