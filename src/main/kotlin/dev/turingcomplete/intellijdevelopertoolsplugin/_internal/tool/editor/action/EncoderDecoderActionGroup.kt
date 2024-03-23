package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.EditorUtils.getSelectedText
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.Encoder
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.executeDecodingInEditor
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.executeEncodingInEditor

internal open class EncoderDecoderActionGroup : DefaultActionGroup("Encoders/Decoders", false) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val encoderActionGroup by lazy {
    createActionGroup(
      title = "Encode To",
      actions = EncodersDecoders.commonEncoders.map { encoder ->
        EncoderAction(encoder) { getSourceText(it) }
      }
    )
  }
  private val decoderActionGroup by lazy {
    createActionGroup(
      title = "Decode To",
      actions = EncodersDecoders.commonDecoders.map { decoder ->
        DecoderAction(decoder) { getSourceText(it) }
      }
    )
  }
  private val encoderDecoderActions: Array<AnAction> by lazy {
    arrayOf(
      encoderActionGroup,
      decoderActionGroup
    )
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  final override fun update(e: AnActionEvent) {
    val editor = e.getData(EDITOR)
    e.presentation.isVisible = editor != null && editor.document.isWritable && getSourceText(e) != null
  }

  final override fun getChildren(e: AnActionEvent?): Array<AnAction> = encoderDecoderActions

  final override fun getActionUpdateThread() = ActionUpdateThread.BGT

  open fun getSourceText(e: AnActionEvent): Pair<String, TextRange>? {
    val editor = e.getData(EDITOR) ?: return null
    return editor.getSelectedText()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createActionGroup(title: String, actions: List<AnAction>) = object : DefaultActionGroup(title, true) {

    private val decoderActions: Array<AnAction> = actions.toTypedArray()

    override fun getChildren(e: AnActionEvent?): Array<AnAction> = decoderActions
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class EncoderAction(
    val encoder: Encoder,
    val getSourceText: (AnActionEvent) -> Pair<String, TextRange>?
  ) : DumbAwareAction(encoder.title, encoder.actionName, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(EDITOR) ?: return
      val (text, textRange) = getSourceText(e) ?: return
      executeEncodingInEditor(text, textRange, encoder, editor)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class DecoderAction(
    val decoder: EncodersDecoders.Decoder,
    val getSourceText: (AnActionEvent) -> Pair<String, TextRange>?
  ) : DumbAwareAction(decoder.title, decoder.actionName, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(EDITOR) ?: return
      val (text, textRange) = getSourceText(e) ?: return
      executeDecodingInEditor(text, textRange, decoder, editor)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}