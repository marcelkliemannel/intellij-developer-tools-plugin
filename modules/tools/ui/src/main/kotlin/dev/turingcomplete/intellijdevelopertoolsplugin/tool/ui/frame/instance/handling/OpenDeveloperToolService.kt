package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.ActionHandlingInstance.TOOL_WINDOW
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.dialog.MainDialogService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.toolwindow.MainToolWindowService

@Service(Service.Level.PROJECT)
class OpenDeveloperToolService(val project: Project) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun <T : OpenDeveloperToolContext> openTool(context: T, reference: OpenDeveloperToolReference<T>) {
    if (showToolWindow()) {
      project.service<MainToolWindowService>().openTool(context, reference)
    }
    else {
      ApplicationManager.getApplication().service<MainDialogService>().openTool(project, context, reference)
    }
  }

  fun showTool(id: String) {
    if (showToolWindow()) {
      project.service<MainToolWindowService>().showTool(id)
    }
    else {
      ApplicationManager.getApplication().service<MainDialogService>().showTool(project, id)
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun showToolWindow(): Boolean =
    if (DeveloperToolsApplicationSettings.generalSettings.autoDetectActionHandlingInstance.get()) {
      !DeveloperToolsApplicationSettings.generalSettings.addOpenMainDialogActionToMainToolbar.get()
    }
    else {
      DeveloperToolsApplicationSettings.generalSettings.selectedActionHandlingInstance.get() == TOOL_WINDOW
    }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
