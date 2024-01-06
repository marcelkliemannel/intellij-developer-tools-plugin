package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.generator

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation

internal class NanoIdGenerator(
  project: Project?,
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : OneLineTextGenerator(
  context = context,
  configuration = configuration,
  parentDisposable = parentDisposable,
  project = project
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun generate(): String = NanoIdUtils.randomNanoId()

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<NanoIdGenerator> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Nano ID",
      contentTitle = "Nano ID Generator"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> NanoIdGenerator) = { configuration ->
      NanoIdGenerator(project, context, configuration, parentDisposable)
    }
  }


  // -- Companion Object -------------------------------------------------------------------------------------------- //
}