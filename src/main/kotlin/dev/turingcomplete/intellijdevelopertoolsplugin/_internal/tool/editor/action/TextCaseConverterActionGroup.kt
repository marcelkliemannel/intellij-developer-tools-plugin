package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.EditorUtils.getSelectedText
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.TextCaseConverter.allTextCases
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.TextCaseConverter.executeConversionInEditor
import dev.turingcomplete.textcaseconverter.TextCase

internal open class TextCaseConverterActionGroup : DefaultActionGroup("Convert Text Case To", true) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val textCasesAction: Array<AnAction> =
    allTextCases.map { ConvertTextCaseAction(it) { getSourceText(it) } }.toTypedArray()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  final override fun update(e: AnActionEvent) {
    e.presentation.isVisible = getSourceText(e) != null
  }

  final override fun getChildren(e: AnActionEvent?): Array<AnAction> = textCasesAction

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  open fun getSourceText(e: AnActionEvent): Pair<String, TextRange>? {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
    return editor.getSelectedText()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ConvertTextCaseAction(
    val textCase: TextCase,
    val getSourceText: (AnActionEvent) -> Pair<String, TextRange>?
  ) : DumbAwareAction(textCase.example(), null, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: return
      val (text, textRange) = getSourceText(e) ?: return
      executeConversionInEditor(text, textRange, textCase, editor)
    }
  }


  // -- Companion Object -------------------------------------------------------------------------------------------- //
}