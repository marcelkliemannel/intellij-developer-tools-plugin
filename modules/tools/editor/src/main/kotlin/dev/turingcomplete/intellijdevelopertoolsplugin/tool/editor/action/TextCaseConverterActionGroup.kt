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
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.getSelectedText
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextCaseUtils.allTextCases
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextCaseUtils.determineWordsSplitter
import dev.turingcomplete.textcaseconverter.TextCase

open class TextCaseConverterActionGroup : DefaultActionGroup("Convert Text Case To", true) {
  // -- Properties ---------------------------------------------------------- //

  private val textCasesAction: Array<AnAction> =
    allTextCases.map { ConvertTextCaseAction(it) { getSourceText(it) } }.toTypedArray()

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  final override fun update(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR)
    e.presentation.isVisible =
      editor != null && editor.document.isWritable && getSourceText(e) != null
  }

  final override fun getChildren(e: AnActionEvent?): Array<AnAction> = textCasesAction

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  open fun getSourceText(e: AnActionEvent): Pair<String, TextRange>? {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
    return editor.getSelectedText()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class ConvertTextCaseAction(
    val textCase: TextCase,
    val getSourceText: (AnActionEvent) -> Pair<String, TextRange>?,
  ) : DumbAwareAction(textCase.example(), null, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: return
      val (text, textRange) = getSourceText(e) ?: return
      executeConversionInEditor(text, textRange, textCase, editor)
    }

    private fun executeConversionInEditor(
      text: String,
      textRange: TextRange,
      textCase: TextCase,
      editor: Editor,
    ) {
      val wordsSplitter = determineWordsSplitter(text, textCase)
      val result = textCase.convert(text, wordsSplitter)
      editor.executeWriteCommand("Convert text case to ${textCase.title().lowercase()}") {
        it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
      }
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
