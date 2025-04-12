package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.FileHandling
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import javax.swing.JComponent

class FileInputOutputHandler(
  id: String,
  val configuration: DeveloperToolConfiguration,
  val context: DeveloperUiToolContext,
  val parentDisposable: Disposable,
  val project: Project?,
) :
  InputOutputHandler(
    id = id,
    title = UiToolsBundle.message("converter.file-input-output-handler.title"),
    errorHolder = ErrorHolder(),
    liveConversionSupported = false,
    textDiffSupported = false,
  ) {
  // -- Properties ---------------------------------------------------------- //

  private lateinit var fileHandling: FileHandling

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun createComponent(): JComponent {
    fileHandling = FileHandling(project, configuration.register("${id}File", "", INPUT))
    return fileHandling.crateComponent(errorHolder)
  }

  override fun read(): ByteArray = fileHandling.readFromFile()

  override fun write(output: ByteArray) {
    fileHandling.writeToFile(output)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
