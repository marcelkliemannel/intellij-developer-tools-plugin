package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import java.awt.event.ItemEvent

internal class LineBreaksEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextConverter(
  presentation = DeveloperToolPresentation("Line Breaks", "Line Breaks Encoder/Decoder"),
  context = encoderDecoderContext,
  configuration = configuration,
  parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var lineBreakDecoding by configuration.register("lineBreakDecoding", LineBreak.CRLF)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun toTarget(text: String): String {
    return when (lineBreakDecoding) {
      LineBreak.CRLF -> StringUtil.convertLineSeparators(text, "\\r\\n")
      LineBreak.LF -> StringUtil.convertLineSeparators(text, "\\n")
    }
  }

  override fun toSource(text: String): String {
    // The target input is not depending on the selected line break decoding,
    // because the user can put anything into the editor without changing the
    // configuration first.
    return text.replace("\\r\\n", System.lineSeparator())
            .replace("\\n", System.lineSeparator())
  }

  override fun Panel.buildConfigurationUi() {
    row {
      label("Decode line break to:").gap(RightGap.SMALL)
      comboBox(LineBreak.values().toList()).configure()
    }
  }

  private fun Cell<ComboBox<LineBreak>>.configure() = this.applyToComponent {
    selectedItem = lineBreakDecoding
    addItemListener { event ->
      if (event.stateChange == ItemEvent.SELECTED) {
        lineBreakDecoding = selectedItem as LineBreak
        transformToTarget()
      }
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

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return LineBreaksEncoderDecoder(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}