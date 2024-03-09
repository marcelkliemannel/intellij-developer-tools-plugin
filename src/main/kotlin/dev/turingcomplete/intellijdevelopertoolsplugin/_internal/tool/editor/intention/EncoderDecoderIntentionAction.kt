package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.intention

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
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.EncoderDecoder
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.TransformationMode
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.encoderDecoders
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.executeTransformationInEditor

internal abstract class EncoderDecoderIntentionAction : IntentionAction, LowPriorityAction {
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
        .createListPopup(EncodeDecodersModeSelectionListPopupStep(text, textRange, editor, encoderDecoders))
        .showInBestPositionFor(editor)
    }
  }

  abstract fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>?

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class EncodeDecodersModeSelectionListPopupStep(
    private val text: String,
    private val textRange: TextRange,
    private val editor: Editor,
    private val encoderDecoders: List<EncoderDecoder>
  ) : BaseListPopupStep<TransformationMode>(null, TransformationMode.entries) {

    override fun hasSubstep(transformationMode: TransformationMode): Boolean = true

    override fun getTextFor(transformationMode: TransformationMode): String = transformationMode.actionName

    override fun onChosen(transformationMode: TransformationMode, finalChoice: Boolean): PopupStep<*> =
      EncodeDecodersListPopupStep(text, textRange, editor, transformationMode, encoderDecoders)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class EncodeDecodersListPopupStep(
    private val text: String,
    private val textRange: TextRange,
    private val editor: Editor,
    private val transformationMode: TransformationMode,
    encoderDecoders: List<EncoderDecoder>
  ) : BaseListPopupStep<EncoderDecoder>(null, encoderDecoders) {

    override fun getTextFor(encoderDecoder: EncoderDecoder): String = encoderDecoder.title

    override fun onChosen(encoderDecoder: EncoderDecoder, finalChoice: Boolean): PopupStep<*>? {
      executeTransformationInEditor(text, textRange, transformationMode, encoderDecoder, editor)
      return super.onChosen(encoderDecoder, finalChoice)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}