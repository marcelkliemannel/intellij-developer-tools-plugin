package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.getSelectedText
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other.TextStatistic

class EditorTextStatisticAction :
  DumbAwareAction(EditorToolsBundle.message("editor-text-statistic-action.title")) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible =
      e.getData(CommonDataKeys.PROJECT) != null && e.getData(CommonDataKeys.EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val text =
      ReadAction.computeBlocking<String, RuntimeException> {
        editor.getSelectedText()?.first ?: editor.document.text
      }
    project
      .service<OpenDeveloperToolService>()
      .openTool(
        TextStatistic.OpenTextStatisticContext(text),
        TextStatistic.openTextStatisticReference,
      )
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
