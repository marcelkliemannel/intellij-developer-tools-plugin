package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextCaseUtils.allTextCases
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextCaseUtils.determineWordsSplitter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EditorSourceText.getSelectedTextsOrTextAtCaret
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle
import dev.turingcomplete.textcaseconverter.TextCase

open class TextCaseConverterActionGroup :
  DefaultActionGroup(EditorToolsBundle.message("text-case-converter-action-group.title"), true) {
  // -- Properties ---------------------------------------------------------- //

  private val textCasesAction: Array<AnAction> =
    allTextCases.map { ConvertTextCaseAction(it) { getSourceTexts(it) } }.toTypedArray()

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  final override fun update(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR)
    e.presentation.isVisible =
      editor != null && editor.document.isWritable && getSourceTexts(e).isNotEmpty()
  }

  final override fun getChildren(e: AnActionEvent?): Array<AnAction> = textCasesAction

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  open fun getSourceTexts(e: AnActionEvent): List<Pair<String, TextRange>> {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return emptyList()
    return editor.getSelectedTextsOrTextAtCaret()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class ConvertTextCaseAction(
    val textCase: TextCase,
    val getSourceTexts: (AnActionEvent) -> List<Pair<String, TextRange>>,
  ) : DumbAwareAction(textCase.example(), null, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: return
      val sourceTexts = getSourceTexts(e)
      if (sourceTexts.isNotEmpty()) {
        executeConversionInEditor(sourceTexts, textCase, editor)
      }
    }

    private fun executeConversionInEditor(
      sourceTexts: List<Pair<String, TextRange>>,
      textCase: TextCase,
      editor: Editor,
    ) {
      val results =
        sourceTexts.map { (text, textRange) ->
          val wordsSplitter = determineWordsSplitter(text, textCase)
          textCase.convert(text, wordsSplitter) to textRange
        }
      editor.executeWriteCommand(
        EditorToolsBundle.message(
          "text-case-converter.action.convert-to",
          textCase.title().lowercase(),
        )
      ) {
        results
          .sortedByDescending { (_, textRange) -> textRange.startOffset }
          .forEach { (result, textRange) ->
            it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
          }
      }
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
