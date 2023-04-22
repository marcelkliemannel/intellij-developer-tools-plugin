package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

interface DeveloperToolFactory<T : DeveloperTool> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun getDeveloperToolContext(): DeveloperToolContext

  fun getDeveloperToolCreator(
    configuration: DeveloperToolConfiguration,
    project: Project?,
    parentDisposable: Disposable
  ): (() -> T)?

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}