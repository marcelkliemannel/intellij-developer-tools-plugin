package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

interface DeveloperToolFactory<T : DeveloperTool> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun getDeveloperToolPresentation(): DeveloperToolPresentation

  fun getDeveloperToolCreator(
    project: Project?,
    parentDisposable: Disposable,
    context: DeveloperToolContext
  ): ((DeveloperToolConfiguration) -> T)?

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}