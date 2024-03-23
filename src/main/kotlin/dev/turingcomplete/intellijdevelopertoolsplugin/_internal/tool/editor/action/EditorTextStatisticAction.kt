package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.EditorUtils.getSelectedText
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other.TextStatistic
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other.TextStatistic.Companion
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.toolwindow.MainToolWindowService

class EditorTextStatisticAction : DumbAwareAction("Text Statistic...") {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.PROJECT) != null
            && e.getData(CommonDataKeys.EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val text = runReadAction { editor.getSelectedText()?.first ?: editor.document.text }
    project.service<MainToolWindowService>().openTool(
      TextStatistic.OpenTextStatisticContext(text),
      Companion.openTextStatisticReference
    )
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}