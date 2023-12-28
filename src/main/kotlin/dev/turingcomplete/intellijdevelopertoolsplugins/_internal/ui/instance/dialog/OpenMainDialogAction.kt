package dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.instance.dialog

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings.DeveloperToolsDialogSettings
import kotlin.concurrent.withLock

class OpenMainDialogAction : DumbAwareAction() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    DeveloperToolsDialogSettings.instance.dialogLock.withLock {
      val currentDialog = DeveloperToolsDialogSettings.instance.currentDialog.get()
      if (currentDialog == null || !currentDialog.isShowing) {
        val mainDialog = MainDialog(e.project)
        DeveloperToolsDialogSettings.instance.currentDialog.set(mainDialog)
        mainDialog.show()
      }
      else {
        currentDialog.toFront()
      }
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}