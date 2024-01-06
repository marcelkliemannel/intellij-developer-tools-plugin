package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation

internal class LineBreaksEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) : TextConverter(
  textConverterContext = encoderDecoderTextConverterContext,
  configuration = configuration,
  parentDisposable = parentDisposable,
  context = context,
  project = project
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var lineBreakDecoding = configuration.register("lineBreakDecoding", LineBreak.CRLF)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun toTarget(text: String) {
    targetText.set(
      when (lineBreakDecoding.get()) {
        LineBreak.CRLF -> StringUtil.convertLineSeparators(text, "\\r\\n")
        LineBreak.LF -> StringUtil.convertLineSeparators(text, "\\n")
      }
    )
  }

  override fun toSource(text: String) {
    // The target input is not depending on the selected line break decoding,
    // because the user can put anything into the editor without changing the
    // configuration first.
    sourceText.set(
      text.replace("\\r\\n", System.lineSeparator()).replace("\\n", System.lineSeparator())
    )
  }

  override fun Panel.buildMiddleFirstConfigurationUi() {
    row {
      comboBox(LineBreak.entries)
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

  class Factory : DeveloperUiToolFactory<LineBreaksEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Line Breaks",
      contentTitle = "Line Breaks Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> LineBreaksEncoderDecoder) =
      { configuration -> LineBreaksEncoderDecoder(configuration, parentDisposable, context, project) }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}