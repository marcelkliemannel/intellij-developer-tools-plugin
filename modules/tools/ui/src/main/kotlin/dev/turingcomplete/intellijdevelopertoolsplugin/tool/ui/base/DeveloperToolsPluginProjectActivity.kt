package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.AddOpenMainDialogActionToMainToolbarTask

class DeveloperToolsPluginProjectActivity : ProjectActivity {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override suspend fun execute(project: Project) {
    if (DeveloperToolsApplicationSettings.generalSettings.addOpenMainDialogActionToMainToolbar.get()) {
      val addOpenMainDialogActionToMainToolbarTask = AddOpenMainDialogActionToMainToolbarTask.createIfAvailable()
      if (addOpenMainDialogActionToMainToolbarTask != null) {
        ApplicationManager.getApplication().invokeLater { addOpenMainDialogActionToMainToolbarTask.run() }
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
