package dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.tool.editor.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.PsiJavaUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.action.EscapeUnescapeActionGroup

class EscapeUnescapeJavaCodeActionGroup : EscapeUnescapeActionGroup() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getSourceTexts(e: AnActionEvent): List<Pair<String, TextRange>> =
    listOfNotNull(
      PsiJavaUtils.getPsiElementAtCaret(e)?.let { PsiJavaUtils.getTextIfStringValue(it) }
    )

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
