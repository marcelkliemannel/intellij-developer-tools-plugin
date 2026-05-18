package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.getSelectedTexts
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.Escaper
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.Unescaper
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.executeEscapeInEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.EscapersUnescapers.executeUnescapeInEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle

open class EscapeUnescapeActionGroup :
  DefaultActionGroup(EditorToolsBundle.message("escape-unescape-action-group.title"), false) {
  // -- Properties ---------------------------------------------------------- //

  private val escapeActionGroup by lazy {
    createActionGroup(
      title = EditorToolsBundle.message("escape-unescape-action-group.escape"),
      actions =
        EscapersUnescapers.commonEscaper.map { escaper ->
          EscapeAction(escaper) { getSourceTexts(it) }
        },
    )
  }
  private val unescapeActionGroup by lazy {
    createActionGroup(
      title = EditorToolsBundle.message("escape-unescape-action-group.unescape"),
      actions =
        EscapersUnescapers.commonUnescaper.map { unescaper ->
          UnescapeAction(unescaper) { getSourceTexts(it) }
        },
    )
  }
  private val encoderDecoderActions: Array<AnAction> by lazy {
    arrayOf(escapeActionGroup, unescapeActionGroup)
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
    return editor.getSelectedTexts()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createActionGroup(title: String, actions: List<AnAction>) =
    object : DefaultActionGroup(title, true) {

      private val decoderActions: Array<AnAction> = actions.toTypedArray()

      override fun getChildren(e: AnActionEvent?): Array<AnAction> = decoderActions
    }

  // -- Inner Type ---------------------------------------------------------- //

  private class EscapeAction(
    val escaper: Escaper,
    val getSourceTexts: (AnActionEvent) -> List<Pair<String, TextRange>>,
  ) : DumbAwareAction(escaper.title, escaper.actionName, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(EDITOR) ?: return
      val sourceTexts = getSourceTexts(e)
      if (sourceTexts.isNotEmpty()) {
        executeEscapeInEditor(sourceTexts, escaper, editor)
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class UnescapeAction(
    val unescaper: Unescaper,
    val getSourceTexts: (AnActionEvent) -> List<Pair<String, TextRange>>,
  ) : DumbAwareAction(unescaper.title, unescaper.actionName, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(EDITOR) ?: return
      val sourceTexts = getSourceTexts(e)
      if (sourceTexts.isNotEmpty()) {
        executeUnescapeInEditor(sourceTexts, unescaper, editor)
      }
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
