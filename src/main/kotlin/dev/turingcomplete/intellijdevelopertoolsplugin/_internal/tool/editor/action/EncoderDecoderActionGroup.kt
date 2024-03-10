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
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.EncoderDecoder
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.TransformationMode.DECODE
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.TransformationMode.ENCODE
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.encoderDecoders
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.EncodersDecoders.executeTransformationInEditor

internal open class EncoderDecoderActionGroup : DefaultActionGroup("Encoders/Decoders", false) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  final override fun update(e: AnActionEvent) {
    val editor = e.getData(EDITOR)
    e.presentation.isVisible = editor != null && editor.document.isWritable && getSourceText(e) != null
  }

  final override fun getChildren(e: AnActionEvent?): Array<AnAction> = arrayOf(
    EncoderDecoderActionsGroup(ENCODE) { getSourceText(it) },
    EncoderDecoderActionsGroup(DECODE) { getSourceText(it) }
  )

  final override fun getActionUpdateThread() = ActionUpdateThread.BGT

  open fun getSourceText(e: AnActionEvent): Pair<String, TextRange>? {
    val editor = e.getData(EDITOR) ?: return null
    return editor.getSelectedText()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class EncoderDecodeAction(
    val transformationMode: EncodersDecoders.TransformationMode,
    val encoderDecoder: EncoderDecoder,
    val getSourceText: (AnActionEvent) -> Pair<String, TextRange>?
  ) : DumbAwareAction(encoderDecoder.title, null, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(EDITOR) ?: return
      val (text, textRange) = getSourceText(e) ?: return
      executeTransformationInEditor(text, textRange, transformationMode, encoderDecoder, editor)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class EncoderDecoderActionsGroup(
    val transformationMode: EncodersDecoders.TransformationMode,
    val getSourceText: (AnActionEvent) -> Pair<String, TextRange>?
  ) : DefaultActionGroup(transformationMode.actionName, true) {

    private val encoderDecoderActions: Array<AnAction> =
      encoderDecoders.map { EncoderDecodeAction(transformationMode, it, getSourceText) }.toTypedArray()

    override fun getChildren(e: AnActionEvent?): Array<AnAction> = encoderDecoderActions
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}