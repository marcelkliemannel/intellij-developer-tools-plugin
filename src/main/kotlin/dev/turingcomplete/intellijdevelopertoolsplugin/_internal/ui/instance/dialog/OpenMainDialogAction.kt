package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.dialog

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import dev.turingcomplete.intellijdevelopertoolsplugin.common.safeCastTo

class OpenMainDialogAction : DumbAwareAction() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    ApplicationManager.getApplication().service<MainDialogService>().openDialog(e.project)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    fun getAction(): OpenMainDialogAction? = ActionManager.getInstance().getAction(ID)?.safeCastTo<OpenMainDialogAction>()

    private const val ID = "dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.dialog.OpenMainDialogAction"
  }
}