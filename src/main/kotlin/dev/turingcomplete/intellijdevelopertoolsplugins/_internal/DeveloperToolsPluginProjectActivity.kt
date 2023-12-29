package dev.turingcomplete.intellijdevelopertoolsplugins._internal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings.DeveloperToolsApplicationSettings

class DeveloperToolsPluginProjectActivity : ProjectActivity {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override suspend fun execute(project: Project) {
    if (DeveloperToolsApplicationSettings.instance.addOpenMainDialogActionToMainToolbar) {
      val addOpenMainDialogActionToMainToolbarTask = AddOpenMainDialogActionToMainToolbarTask.createIfAvailable()
      if (addOpenMainDialogActionToMainToolbarTask != null) {
        ApplicationManager.getApplication().invokeLater { addOpenMainDialogActionToMainToolbarTask.run() }
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}