package dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.tool.editor.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.PsiKotlinUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.action.EscapeUnescapeActionGroup

class EscapeUnescapeKotlinCodeActionGroup : EscapeUnescapeActionGroup() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getSourceTexts(e: AnActionEvent): List<Pair<String, TextRange>> =
    listOfNotNull(PsiKotlinUtils.getTextFromStringValue(e))

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
