package dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.tool.editor.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.tool.editor.action.EncodeDecodeActionGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.PsiKotlinUtils

internal class EncodeDecodeKotlinCodeActionGroup : EncodeDecodeActionGroup() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getSourceText(e: AnActionEvent): Pair<String, TextRange>? =
    PsiKotlinUtils.getTextFromStringValueOrIdentifier(e)

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}