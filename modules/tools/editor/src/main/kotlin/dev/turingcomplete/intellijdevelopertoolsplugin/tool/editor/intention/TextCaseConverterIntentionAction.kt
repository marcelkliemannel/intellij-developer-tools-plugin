package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextCaseUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextCaseUtils.allTextCases
import dev.turingcomplete.textcaseconverter.TextCase

abstract class TextCaseConverterIntentionAction : IntentionAction, LowPriorityAction {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  final override fun startInWriteAction() = false

  final override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) =
    editor != null && file != null && editor.document.isWritable && getSourceText(editor, file) != null

  final override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (editor == null || file == null) {
      return
    }

    val (text, textRange) = getSourceText(editor, file) ?: return

    ApplicationManager.getApplication().invokeLater {
      JBPopupFactory.getInstance()
        .createListPopup(TextCaseListPopupStep(editor, text, textRange))
        .showInBestPositionFor(editor)
    }
  }

  abstract fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>?

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class TextCaseListPopupStep(
    private val editor: Editor,
    private val text: String,
    private val textRange: TextRange
  ) : BaseListPopupStep<TextCase>("Select Target Text Case", allTextCases) {

    override fun getTextFor(textCase: TextCase): String = textCase.example()

    override fun onChosen(textCase: TextCase, finalChoice: Boolean): PopupStep<*>? {
      val wordsSplitter = TextCaseUtils.determineWordsSplitter(text, textCase)
      val result = textCase.convert(text, wordsSplitter)
      editor.executeWriteCommand("Convert text case to ${textCase.title().lowercase()}") {
        it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
      }

      return super.onChosen(textCase, finalChoice)
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
