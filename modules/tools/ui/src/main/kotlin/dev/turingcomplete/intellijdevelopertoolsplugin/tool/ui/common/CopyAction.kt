package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import java.awt.datatransfer.StringSelection

class CopyAction(private val contentDataKey: DataKey<String> = CONTENT_DATA_KEY) :
  DumbAwareAction(
    "Copy to Clipboard",
    "Copy the text into the system clipboard",
    AllIcons.Actions.Copy,
  ) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    val content = e.getData(contentDataKey) ?: error("Data missing for: ${contentDataKey.name}")
    CopyPasteManager.getInstance().setContents(StringSelection(content))
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    val CONTENT_DATA_KEY = DataKey.create<String>("content")
  }
}
