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
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders.Decoder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders.Encoder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders.commonDecoders
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders.commonEncoders
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders.executeDecodingInEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders.executeEncodingInEditor

abstract class EncodeDecodeIntentionAction : IntentionAction, LowPriorityAction {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  final override fun startInWriteAction(): Boolean = false

  final override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
    editor != null &&
      file != null &&
      editor.document.isWritable &&
      getSourceText(editor, file) != null

  final override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (editor == null || file == null) {
      return
    }

    val (text, textRange) = getSourceText(editor, file) ?: return

    ApplicationManager.getApplication().invokeLater {
      JBPopupFactory.getInstance()
        .createListPopup(EncodersDecodersModeSelectionListPopupStep(text, textRange, editor))
        .showInBestPositionFor(editor)
    }
  }

  abstract fun getSourceText(editor: Editor, file: PsiFile): Pair<String, TextRange>?

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class EncodersDecodersModeSelectionListPopupStep(
    text: String,
    textRange: TextRange,
    editor: Editor,
  ) :
    BaseListPopupStep<EncoderDecoderListPopupStep<*>>(
      null,
      EncoderListPopupStep(text, textRange, editor),
      DecoderListPopupStep(text, textRange, editor),
    ) {

    override fun hasSubstep(baseListPopupStep: EncoderDecoderListPopupStep<*>): Boolean = true

    override fun getTextFor(baseListPopupStep: EncoderDecoderListPopupStep<*>): String =
      baseListPopupStep.actionName

    override fun onChosen(
      baseListPopupStep: EncoderDecoderListPopupStep<*>,
      finalChoice: Boolean,
    ): PopupStep<*> = baseListPopupStep
  }

  // -- Inner Type ---------------------------------------------------------- //

  interface EncoderDecoderListPopupStep<T> : PopupStep<T> {

    val actionName: String
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class EncoderListPopupStep(
    private val text: String,
    private val textRange: TextRange,
    private val editor: Editor,
  ) : BaseListPopupStep<Encoder>(null, commonEncoders), EncoderDecoderListPopupStep<Encoder> {

    override val actionName: String = "Encode To"

    override fun getTextFor(encoder: Encoder): String = encoder.title

    override fun onChosen(encoder: Encoder, finalChoice: Boolean): PopupStep<*>? {
      executeEncodingInEditor(text, textRange, encoder, editor)
      return super.onChosen(encoder, finalChoice)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class DecoderListPopupStep(
    private val text: String,
    private val textRange: TextRange,
    private val editor: Editor,
  ) : BaseListPopupStep<Decoder>(null, commonDecoders), EncoderDecoderListPopupStep<Decoder> {

    override val actionName: String = "Decode From"

    override fun getTextFor(decoder: Decoder): String = decoder.title

    override fun onChosen(decoder: Decoder, finalChoice: Boolean): PopupStep<*>? {
      executeDecodingInEditor(text, textRange, decoder, editor)
      return super.onChosen(decoder, finalChoice)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
