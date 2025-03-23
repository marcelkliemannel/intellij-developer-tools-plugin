package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.executeWriteCommand
import org.apache.commons.text.StringEscapeUtils

object EscapersUnescapers {
  // -- Variables ----------------------------------------------------------- //

  private val log = logger<EscapersUnescapers>()

  val commonEscaper =
    listOf(
      Escaper("Java String", { StringEscapeUtils.escapeJava(it) }),
      Escaper("HTML Entities", { StringEscapeUtils.escapeHtml4(it) }),
      Escaper("JSON Value", { StringEscapeUtils.escapeJson(it) }),
      Escaper("XML Value", { StringEscapeUtils.escapeXml11(it) }),
      Escaper("CSV Value", { StringEscapeUtils.escapeCsv(it) }),
    )

  val commonUnescaper =
    listOf(
      Unescaper("Java String", { StringEscapeUtils.unescapeJava(it) }),
      Unescaper("HTML Entities", { StringEscapeUtils.escapeHtml4(it) }),
      Unescaper("JSON Value", { StringEscapeUtils.unescapeJson(it) }),
      Unescaper("XML Value", { StringEscapeUtils.unescapeCsv(it) }),
      Unescaper("CSV Value", { StringEscapeUtils.unescapeCsv(it) }),
    )

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun executeEscapeInEditor(text: String, textRange: TextRange, escaper: Escaper, editor: Editor) {
    try {
      val result = escaper.escape(text)
      editor.executeWriteCommand(escaper.actionName) {
        it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
      }
    } catch (e: Exception) {
      log.warn("Escape failed", e)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(editor.project, "Escape failed: ${e.message}", escaper.actionName)
      }
    }
  }

  fun executeUnescapeInEditor(
    text: String,
    textRange: TextRange,
    unescaper: Unescaper,
    editor: Editor,
  ) {
    try {
      val result = unescaper.unescape(text)
      editor.executeWriteCommand(unescaper.actionName) {
        it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
      }
    } catch (e: Exception) {
      log.warn("Unescape failed", e)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(
          editor.project,
          "Unescape failed: ${e.message}",
          unescaper.actionName,
        )
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Escaper(
    val title: String,
    val escape: (String) -> String,
    val actionName: String = "Escape $title",
  )

  // -- Inner Type ---------------------------------------------------------- //

  class Unescaper(
    val title: String,
    val unescape: (String) -> String,
    val actionName: String = "Unescape $title",
  )
}
