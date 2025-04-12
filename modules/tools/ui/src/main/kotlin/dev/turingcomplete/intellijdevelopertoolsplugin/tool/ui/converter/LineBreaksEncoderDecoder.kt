package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

class LineBreaksEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("line-breaks-encoder-decoder.title"),
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var lineBreakDecoding = configuration.register("lineBreakDecoding", LineBreak.CRLF)

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    when (lineBreakDecoding.get()) {
      LineBreak.CRLF -> StringUtil.convertLineSeparators(String(source), "\\r\\n")
      LineBreak.LF -> StringUtil.convertLineSeparators(String(source), "\\n")
    }.toByteArray()

  /**
   * The target input is not depending on the selected line break decoding, because the user can put
   * anything into the editor without changing the configuration first.
   */
  override fun doConvertToSource(target: ByteArray): ByteArray =
    String(target)
      .replace("\\r\\n", System.lineSeparator())
      .replace("\\n", System.lineSeparator())
      .toByteArray()

  override fun Panel.buildTargetTopConfigurationUi() {
    row {
      comboBox(LineBreak.entries)
        .label(UiToolsBundle.message("line-breaks-encoder-decoder.line-break-decoding"))
        .bindItem(lineBreakDecoding)
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private enum class LineBreak(val displayText: String) {

    CRLF("\\r\\n"),
    LF("\\n");

    override fun toString(): String = displayText
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<LineBreaksEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("line-breaks-encoder-decoder.title"),
        groupedMenuTitle = UiToolsBundle.message("line-breaks-encoder-decoder.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("line-breaks-encoder-decoder.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> LineBreaksEncoderDecoder) = { configuration ->
      LineBreaksEncoderDecoder(configuration, parentDisposable, context, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
