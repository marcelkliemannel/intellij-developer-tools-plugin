package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EditorSourceText.getSelectedTextsOrTextAtCaret
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders.Encoder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders.executeDecodingInEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EncodersDecoders.executeEncodingInEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle

open class EncodeDecodeActionGroup :
  DefaultActionGroup(EditorToolsBundle.message("encode-decode-action-group.title"), false) {
  // -- Properties ---------------------------------------------------------- //

  private val encoderActionGroup by lazy {
    createActionGroup(
      title = EditorToolsBundle.message("encode-decode-action-group.encode-to"),
      actions =
        EncodersDecoders.commonEncoders.map { encoder ->
          EncoderAction(encoder) { getSourceTexts(it) }
        },
    )
  }
  private val decoderActionGroup by lazy {
    createActionGroup(
      title = EditorToolsBundle.message("encode-decode-action-group.decode-from"),
      actions =
        EncodersDecoders.commonDecoders.map { decoder ->
          DecoderAction(decoder) { getSourceTexts(it) }
        },
    )
  }
  private val encoderDecoderActions: Array<AnAction> by lazy {
    arrayOf(encoderActionGroup, decoderActionGroup)
  }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  final override fun update(e: AnActionEvent) {
    val editor = e.getData(EDITOR)
    e.presentation.isVisible =
      editor != null && editor.document.isWritable && getSourceTexts(e).isNotEmpty()
  }

  final override fun getChildren(e: AnActionEvent?): Array<AnAction> = encoderDecoderActions

  final override fun getActionUpdateThread() = ActionUpdateThread.BGT

  open fun getSourceTexts(e: AnActionEvent): List<Pair<String, TextRange>> {
    val editor = e.getData(EDITOR) ?: return emptyList()
    return editor.getSelectedTextsOrTextAtCaret()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createActionGroup(title: String, actions: List<AnAction>) =
    object : DefaultActionGroup(title, true) {

      private val decoderActions: Array<AnAction> = actions.toTypedArray()

      override fun getChildren(e: AnActionEvent?): Array<AnAction> = decoderActions
    }

  // -- Inner Type ---------------------------------------------------------- //

  private class EncoderAction(
    val encoder: Encoder,
    val getSourceTexts: (AnActionEvent) -> List<Pair<String, TextRange>>,
  ) : DumbAwareAction(encoder.title, encoder.actionName, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(EDITOR) ?: return
      val sourceTexts = getSourceTexts(e)
      if (sourceTexts.isNotEmpty()) {
        executeEncodingInEditor(sourceTexts, encoder, editor)
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class DecoderAction(
    val decoder: EncodersDecoders.Decoder,
    val getSourceTexts: (AnActionEvent) -> List<Pair<String, TextRange>>,
  ) : DumbAwareAction(decoder.title, decoder.actionName, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(EDITOR) ?: return
      val sourceTexts = getSourceTexts(e)
      if (sourceTexts.isNotEmpty()) {
        executeDecodingInEditor(sourceTexts, decoder, editor)
      }
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
