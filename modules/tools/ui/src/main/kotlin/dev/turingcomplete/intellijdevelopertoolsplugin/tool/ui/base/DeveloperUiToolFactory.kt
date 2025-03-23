package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration

interface DeveloperUiToolFactory<T : DeveloperUiTool> {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun getDeveloperUiToolPresentation(): DeveloperUiToolPresentation

  fun getDeveloperUiToolCreator(
    project: Project?,
    parentDisposable: Disposable,
    context: DeveloperUiToolContext
  ): ((DeveloperToolConfiguration) -> T)?

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
