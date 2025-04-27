package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.FileHandling
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.FileHandling.WriteFormat
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import javax.swing.JComponent

class FileInputOutputHandler(
  id: String,
  val configuration: DeveloperToolConfiguration,
  val context: DeveloperUiToolContext,
  val parentDisposable: Disposable,
  val project: Project?,
  inputOutputDirection: InputOutputDirection,
) :
  InputOutputHandler(
    id = id,
    title = UiToolsBundle.message("converter.file-input-output-handler.title"),
    errorHolder = ErrorHolder(),
    liveConversionSupported = false,
    textDiffSupported = false,
    inputOutputDirection = inputOutputDirection,
  ) {
  // -- Properties ---------------------------------------------------------- //

  private lateinit var fileHandling: FileHandling

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun createComponent(): JComponent {
    fileHandling =
      FileHandling(
        project = project,
        file = configuration.register("${id}File", "", INPUT),
        writeFormat =
          configuration.register("${id}FileWriteFormat", WriteFormat.BINARY, CONFIGURATION),
        supportsWrite = inputOutputDirection.supportsWrite,
      )
    return fileHandling.crateComponent(errorHolder)
  }

  override fun read(): ByteArray {
    check(inputOutputDirection.supportsRead)
    return fileHandling.readFromFile()
  }

  override fun write(output: ByteArray) {
    check(inputOutputDirection.supportsWrite)
    fileHandling.writeToFile(output)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
