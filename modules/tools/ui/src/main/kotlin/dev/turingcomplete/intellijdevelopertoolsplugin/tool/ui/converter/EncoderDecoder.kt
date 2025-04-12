package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.Converter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

abstract class EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
  title: String,
) :
  Converter(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = title,
    sourceTitle = UiToolsBundle.message("encoder-decoder.source-title"),
    targetTitle = UiToolsBundle.message("encoder-decoder.target-title"),
    toSourceTitle = UiToolsBundle.message("encoder-decoder.to-source-title"),
    toTargetTitle = UiToolsBundle.message("encoder-decoder.to-target-title"),
  ) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
