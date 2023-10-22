package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.system.OS
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import java.lang.System.lineSeparator

internal class CliCommandConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperToolContext,
  project: Project?
) : TextConverter(
  textConverterContext = TextConverterContext(
    convertActionTitle = "Add line breaks",
    revertActionTitle = "Remove line breaks",
    sourceTitle = "Without line breaks",
    targetTitle = "With line breaks",
    diffSupport = DiffSupport(
      title = "CLI Command Formatting"
    ),
    defaultSourceText = "python script.py --input-file=input.txt --output-file=output.txt --verbose"
  ),
  configuration = configuration,
  parentDisposable = parentDisposable,
  context = context,
  project = project
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val lineBreakDelimiter = configuration.register("lineBreakDelimiter", defaultLineBreakDelimiter)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun Panel.buildMiddleFirstConfigurationUi() {
    row {
      textField()
        .label("Line break delimiter:")
        .bindText(lineBreakDelimiter)
    }
  }

  override fun toTarget(text: String) {
    val lines = mutableListOf<String>()

    var currentLine = ""
    cliCommandAllArgumentsSplitRegex
      .findAll(text)
      .mapNotNull { matchResult ->
        matchResult.groups[1]?.let { "\"${it.value}\"" } ?: matchResult.groups[2]?.let { "'${it.value}'" } ?: matchResult.groups[0]?.value
      }
      .map { it.trim() }
      .forEach { token ->
        currentLine = if (token.startsWith("-")) {
          lines.add(currentLine)

          if (lines.size >= 1) {
            "$CLI_COMMAND_INDENT$token"
          }
          else {
            token
          }
        }
        else if (currentLine.isBlank()) {
          token
        }
        else {
          "$currentLine $token"
        }
      }
    lines.add(currentLine)

    val result = lines.filter { it.isNotBlank() }
      .joinToString(" ${lineBreakDelimiter.get()}${lineSeparator()}")
    targetText.set(result)
  }

  override fun toSource(text: String) {
    val lineBreakDelimiterValue = lineBreakDelimiter.get()
    val result = cliCommandAllArgumentsSplitRegex
      .findAll(text)
      .mapNotNull { matchResult ->
        matchResult.groups[1]?.let { "\"${it.value}\"" } ?: matchResult.groups[2]?.let { "'${it.value}'" } ?: matchResult.groups[0]?.value
      }
      .map { it.trim() }
      .filter { it != lineBreakDelimiterValue }
      .joinToString(" ")
    sourceText.set(result)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<CliCommandConverter> {

    override fun getDeveloperToolPresentation() = DeveloperToolPresentation(
      menuTitle = "CLI Command",
      contentTitle = "CLI Command Line Breaks"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperToolContext
    ): ((DeveloperToolConfiguration) -> CliCommandConverter) =
      { configuration -> CliCommandConverter(configuration, parentDisposable, context, project) }
  }


  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val CLI_COMMAND_INDENT = "  "
    private val defaultLineBreakDelimiter = if (OS.CURRENT == OS.Windows) "^" else "\\"
    private val cliCommandAllArgumentsSplitRegex = Regex("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")
  }
}