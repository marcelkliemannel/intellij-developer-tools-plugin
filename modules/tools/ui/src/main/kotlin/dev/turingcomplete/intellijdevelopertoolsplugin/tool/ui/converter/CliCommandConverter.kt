package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.system.OS
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.BidirectionalConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.lang.System.lineSeparator

class CliCommandConverter(
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
    title = UiToolsBundle.message("cli-command-converter.title"),
    sourceTitle = UiToolsBundle.message("cli-command-converter.source-title"),
    targetTitle = UiToolsBundle.message("cli-command-converter.target-title"),
    toSourceTitle = UiToolsBundle.message("cli-command-converter.to-source-title"),
    toTargetTitle = UiToolsBundle.message("cli-command-converter.to-target-title"),
  ) {
  // -- Properties ---------------------------------------------------------- //

  private val lineBreakDelimiter =
    configuration.register("lineBreakDelimiter", defaultLineBreakDelimiter)

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildTargetTopConfigurationUi() {
    row {
      textField()
        .label(UiToolsBundle.message("cli-command-converter.line-break-delimiter"))
        .bindText(lineBreakDelimiter)
    }
  }

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultSourceInputOutputHandlerId,
      exampleText = EXAMPLE_SOURCE_TEXT,
    )
  }

  override fun ConversionSideHandler.addTargetTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultTargetInputOutputHandlerId,
      exampleText = String(doConvertToTarget(EXAMPLE_SOURCE_TEXT.toByteArray())),
    )
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray {
    val lines = mutableListOf<String>()

    var currentLine = ""
    cliCommandAllArgumentsSplitRegex
      .findAll(String(source))
      .mapNotNull { matchResult ->
        matchResult.groups[1]?.let { "\"${it.value}\"" }
          ?: matchResult.groups[2]?.let { "'${it.value}'" }
          ?: matchResult.groups[0]?.value
      }
      .map { it.trim() }
      .forEach { token ->
        currentLine =
          if (token.startsWith("-")) {
            lines.add(currentLine)

            if (lines.isNotEmpty()) {
              "$CLI_COMMAND_INDENT$token"
            } else {
              token
            }
          } else if (currentLine.isBlank()) {
            token
          } else {
            "$currentLine $token"
          }
      }
    lines.add(currentLine)

    return lines
      .filter { it.isNotBlank() }
      .joinToString(" ${lineBreakDelimiter}${lineSeparator()}")
      .toByteArray()
  }

  override fun doConvertToSource(target: ByteArray): ByteArray =
    cliCommandAllArgumentsSplitRegex
      .findAll(String(target))
      .mapNotNull { matchResult ->
        matchResult.groups[1]?.let { "\"${it.value}\"" }
          ?: matchResult.groups[2]?.let { "'${it.value}'" }
          ?: matchResult.groups[0]?.value
      }
      .map { it.trim() }
      .filter { it != lineBreakDelimiter.get() }
      .joinToString(" ")
      .toByteArray()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<CliCommandConverter> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("cli-command-converter.title"),
        contentTitle = UiToolsBundle.message("cli-command-converter.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> CliCommandConverter) = { configuration ->
      CliCommandConverter(configuration, parentDisposable, context, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val CLI_COMMAND_INDENT = "  "
    private val defaultLineBreakDelimiter = if (OS.CURRENT == OS.Windows) "^" else "\\"
    private val cliCommandAllArgumentsSplitRegex = Regex("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")

    private const val EXAMPLE_SOURCE_TEXT =
      "python script.py --input-file=input.txt --output-file=output.txt --verbose"
  }
}
