package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.handling

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings.ActionHandlingInstance.TOOL_WINDOW
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.dialog.MainDialogService
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.toolwindow.MainToolWindowService

@Service(Service.Level.PROJECT)
internal class OpenDeveloperToolService(val project: Project) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

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

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun showToolWindow(): Boolean {
    val showToolWindow = if (DeveloperToolsApplicationSettings.instance.autoDetectActionHandlingInstance) {
      !DeveloperToolsApplicationSettings.instance.addOpenMainDialogActionToMainToolbar
    }
    else {
      DeveloperToolsApplicationSettings.instance.selectedActionHandlingInstance == TOOL_WINDOW
    }
    return showToolWindow
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}