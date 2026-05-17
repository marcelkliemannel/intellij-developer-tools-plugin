package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle
import org.apache.commons.text.StringEscapeUtils

object EscapersUnescapers {
  // -- Variables ----------------------------------------------------------- //

  private val log = logger<EscapersUnescapers>()

  val commonEscaper =
    listOf(
      Escaper(
        EditorToolsBundle.message("escape-unescape.title.java-string"),
        { StringEscapeUtils.escapeJava(it) },
      ),
      Escaper(
        EditorToolsBundle.message("escape-unescape.title.html-entities"),
        { StringEscapeUtils.escapeHtml4(it) },
      ),
      Escaper(
        EditorToolsBundle.message("escape-unescape.title.json-value"),
        { StringEscapeUtils.escapeJson(it) },
      ),
      Escaper(
        EditorToolsBundle.message("escape-unescape.title.xml-value"),
        { StringEscapeUtils.escapeXml11(it) },
      ),
      Escaper(
        EditorToolsBundle.message("escape-unescape.title.csv-value"),
        { StringEscapeUtils.escapeCsv(it) },
      ),
    )

  val commonUnescaper =
    listOf(
      Unescaper(
        EditorToolsBundle.message("escape-unescape.title.java-string"),
        { StringEscapeUtils.unescapeJava(it) },
      ),
      Unescaper(
        EditorToolsBundle.message("escape-unescape.title.html-entities"),
        { StringEscapeUtils.unescapeHtml4(it) },
      ),
      Unescaper(
        EditorToolsBundle.message("escape-unescape.title.json-value"),
        { StringEscapeUtils.unescapeJson(it) },
      ),
      Unescaper(
        EditorToolsBundle.message("escape-unescape.title.xml-value"),
        { StringEscapeUtils.unescapeXml(it) },
      ),
      Unescaper(
        EditorToolsBundle.message("escape-unescape.title.csv-value"),
        { StringEscapeUtils.unescapeCsv(it) },
      ),
    )

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun executeEscapeInEditor(text: String, textRange: TextRange, escaper: Escaper, editor: Editor) {
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        val result = escaper.escape(text)
        ApplicationManager.getApplication().invokeLater {
          if (!editor.isDisposed && editor.document.isWritable) {
            editor.executeWriteCommand(escaper.actionName) {
              it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
            }
          }
        }
      } catch (e: Exception) {
        log.warn("Escape failed", e)
        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog(
            editor.project,
            EditorToolsBundle.message("escape-unescape.error.escape-failed", e.message ?: ""),
            escaper.actionName,
          )
        }
      }
    }
  }

  fun executeUnescapeInEditor(
    text: String,
    textRange: TextRange,
    unescaper: Unescaper,
    editor: Editor,
  ) {
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        val result = unescaper.unescape(text)
        ApplicationManager.getApplication().invokeLater {
          if (!editor.isDisposed && editor.document.isWritable) {
            editor.executeWriteCommand(unescaper.actionName) {
              it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
            }
          }
        }
      } catch (e: Exception) {
        log.warn("Unescape failed", e)
        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog(
            editor.project,
            EditorToolsBundle.message("escape-unescape.error.unescape-failed", e.message ?: ""),
            unescaper.actionName,
          )
        }
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Escaper(
    val title: String,
    val escape: (String) -> String,
    val actionName: String = EditorToolsBundle.message("escape-unescape.action.escape", title),
  )

  // -- Inner Type ---------------------------------------------------------- //

  class Unescaper(
    val title: String,
    val unescape: (String) -> String,
    val actionName: String = EditorToolsBundle.message("escape-unescape.action.unescape", title),
  )
}
