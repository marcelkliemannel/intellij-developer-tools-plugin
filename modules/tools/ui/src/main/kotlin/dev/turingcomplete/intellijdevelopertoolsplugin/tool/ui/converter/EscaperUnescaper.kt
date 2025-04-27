package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.BidirectionalConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

abstract class EscaperUnescaper(
  title: String,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  BidirectionalConverter(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = title,
    sourceTitle = UiToolsBundle.message("escaper-unescaper.source-title"),
    targetTitle = UiToolsBundle.message("escaper-unescaper.target-title"),
    toSourceTitle = UiToolsBundle.message("escaper-unescaper.to-source-title"),
    toTargetTitle = UiToolsBundle.message("escaper-unescaper.to-target-title"),
  ) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
