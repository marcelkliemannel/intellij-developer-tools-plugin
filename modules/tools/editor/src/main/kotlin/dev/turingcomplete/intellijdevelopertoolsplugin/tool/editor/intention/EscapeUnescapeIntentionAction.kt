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
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.Escaper
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.Unescaper
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.commonEscaper
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.commonUnescaper
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.executeEscapeInEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.executeUnescapeInEditor

abstract class EscapeUnescapeIntentionAction : IntentionAction, LowPriorityAction {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  final override fun startInWriteAction(): Boolean = false

  final override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
    editor != null && file != null
            && editor.document.isWritable
            && getSourceText(editor, file) != null

  final override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (editor == null || file == null) {
      return
    }

    val (text, textRange) = getSourceText(editor, file) ?: return

    ApplicationManager.getApplication().invokeLater {
      JBPopupFactory.getInstance()
        .createListPopup(EscapersUnescapersModeSelectionListPopupStep(text, textRange, editor))
        .showInBestPositionFor(editor)
    }
  }

  abstract fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>?

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class EscapersUnescapersModeSelectionListPopupStep(
    text: String,
    textRange: TextRange,
    editor: Editor
  ) : BaseListPopupStep<EscaperUnescaperListPopupStep<*>>(
    null,
    EscaperListPopupStep(text, textRange, editor),
    UnescaperListPopupStep(text, textRange, editor)
  ) {

    override fun hasSubstep(baseListPopupStep: EscaperUnescaperListPopupStep<*>): Boolean = true

    override fun getTextFor(baseListPopupStep: EscaperUnescaperListPopupStep<*>): String = baseListPopupStep.actionName

    override fun onChosen(baseListPopupStep: EscaperUnescaperListPopupStep<*>, finalChoice: Boolean): PopupStep<*> =
      baseListPopupStep
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  interface EscaperUnescaperListPopupStep<T> : PopupStep<T> {

    val actionName: String
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class EscaperListPopupStep(
    private val text: String,
    private val textRange: TextRange,
    private val editor: Editor
  ) : BaseListPopupStep<Escaper>(null, commonEscaper), EscaperUnescaperListPopupStep<Escaper> {

    override val actionName: String = "Escape"

    override fun getTextFor(escaper: Escaper): String = escaper.title

    override fun onChosen(escaper: Escaper, finalChoice: Boolean): PopupStep<*>? {
      executeEscapeInEditor(text, textRange, escaper, editor)
      return super.onChosen(escaper, finalChoice)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class UnescaperListPopupStep(
    private val text: String,
    private val textRange: TextRange,
    private val editor: Editor
  ) : BaseListPopupStep<Unescaper>(null, commonUnescaper), EscaperUnescaperListPopupStep<Unescaper> {

    override val actionName: String = "Unescape"

    override fun getTextFor(unescaper: Unescaper): String = unescaper.title

    override fun onChosen(unescaper: Unescaper, finalChoice: Boolean): PopupStep<*>? {
      executeUnescapeInEditor(text, textRange, unescaper, editor)
      return super.onChosen(unescaper, finalChoice)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
