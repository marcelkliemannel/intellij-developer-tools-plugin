package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.generator

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

class NanoIdGenerator(
  project: Project?,
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
) :
  OneLineTextGenerator(
    context = context,
    configuration = configuration,
    parentDisposable = parentDisposable,
    project = project,
  ) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun generate(): String = NanoIdUtils.randomNanoId()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<NanoIdGenerator> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("nano-id-generator.menu-title"),
        contentTitle = UiToolsBundle.message("nano-id-generator.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> NanoIdGenerator) = { configuration ->
      NanoIdGenerator(project, context, configuration, parentDisposable)
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
