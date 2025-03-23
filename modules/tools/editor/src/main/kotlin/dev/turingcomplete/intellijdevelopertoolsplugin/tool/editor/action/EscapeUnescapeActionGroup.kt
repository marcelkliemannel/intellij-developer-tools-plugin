package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.getSelectedText
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.Escaper
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.Unescaper
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.executeEscapeInEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.executeUnescapeInEditor

open class EscapeUnescapeActionGroup : DefaultActionGroup("Escape/Unescape", false) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val escapeActionGroup by lazy {
    createActionGroup(
      title = "Escape",
      actions = EscapersUnescapers.commonEscaper.map { escaper ->
        EscapeAction(escaper) { getSourceText(it) }
      }
    )
  }
  private val unescapeActionGroup by lazy {
    createActionGroup(
      title = "Unescape",
      actions = EscapersUnescapers.commonUnescaper.map { unescaper ->
        UnescapeAction(unescaper) { getSourceText(it) }
      }
    )
  }
  private val encoderDecoderActions: Array<AnAction> by lazy {
    arrayOf(
      escapeActionGroup,
      unescapeActionGroup
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

  private class EscapeAction(
    val escaper: Escaper,
    val getSourceText: (AnActionEvent) -> Pair<String, TextRange>?
  ) : DumbAwareAction(escaper.title, escaper.actionName, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(EDITOR) ?: return
      val (text, textRange) = getSourceText(e) ?: return
      executeEscapeInEditor(text, textRange, escaper, editor)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class UnescapeAction(
    val unescaper: Unescaper,
    val getSourceText: (AnActionEvent) -> Pair<String, TextRange>?
  ) : DumbAwareAction(unescaper.title, unescaper.actionName, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(EDITOR) ?: return
      val (text, textRange) = getSourceText(e) ?: return
      executeUnescapeInEditor(text, textRange, unescaper, editor)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
