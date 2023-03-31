package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory

internal class LineBreaksEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextConverter(
  developerToolContext = DeveloperToolContext("Line Breaks", "Line Breaks Encoder/Decoder"),
  textConverterContext = encoderDecoderTextConverterContext,
  configuration = configuration,
  parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var lineBreakDecoding = configuration.register("lineBreakDecoding", LineBreak.CRLF)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun toTarget(text: String) {
    targetText = when (lineBreakDecoding.get()) {
      LineBreak.CRLF -> StringUtil.convertLineSeparators(text, "\\r\\n")
      LineBreak.LF -> StringUtil.convertLineSeparators(text, "\\n")
    }
  }

  override fun toSource(text: String) {
    // The target input is not depending on the selected line break decoding,
    // because the user can put anything into the editor without changing the
    // configuration first.
    sourceText = text.replace("\\r\\n", System.lineSeparator())
      .replace("\\n", System.lineSeparator())
  }

  override fun Panel.buildMiddleFirstConfigurationUi() {
    row {
      comboBox(LineBreak.values().toList())
        .label("Decode line break to:")
        .bindItem(lineBreakDecoding)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class LineBreak(val title: String) {

    CRLF("\\r\\n"),
    LF("\\n");

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<LineBreaksEncoderDecoder> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = LineBreaksEncoderDecoder(configuration, parentDisposable)
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}