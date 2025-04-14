package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

class EscapeSequencesEscaperUnescaper(
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
    title = UiToolsBundle.message("escape-sequences-escaper-unescaper.title"),
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var lineBreaksEncodingEnabled = configuration.register("lineBreaksEncodingEnabled", true)
  private var lineBreaksEncodingEscapeSequence =
    configuration.register("lineBreaksEscapeSequence", LineBreak.CRLF)
  private var tabsEncodingEnabled = configuration.register("tabsEncodingEnabled", true)
  private var backslashsEncodingEnabled = configuration.register("backslashsEncodingEnabled", true)
  private var singleQuotesEncodingEnabled =
    configuration.register("singleQuotesEncodingEnabled", true)
  private var doubleQuotesEncodingEnabled =
    configuration.register("doubleQuotesEncodingEnabled", true)

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun doConvertToTarget(source: ByteArray): ByteArray {
    var text = String(source)

    if (backslashsEncodingEnabled.get()) {
      text = text.replace("\\", "\\\\")
    }

    if (lineBreaksEncodingEnabled.get()) {
      text =
        when (lineBreaksEncodingEscapeSequence.get()) {
          LineBreak.CRLF -> StringUtil.convertLineSeparators(text, "\\r\\n")
          LineBreak.LF -> StringUtil.convertLineSeparators(text, "\\n")
        }
    }

    if (tabsEncodingEnabled.get()) {
      text = text.replace("\t", "\\t")
    }

    if (singleQuotesEncodingEnabled.get()) {
      text = text.replace("'", "\\'")
    }

    if (doubleQuotesEncodingEnabled.get()) {
      text = text.replace("\"", "\\\"")
    }

    return text.toByteArray()
  }

  /**
   * The target input is not depending on the selected line break decoding, because the user can put
   * anything into the editor without changing the configuration first.
   */
  override fun doConvertToSource(target: ByteArray): ByteArray {
    var text = String(target)

    if (lineBreaksEncodingEnabled.get()) {
      text = text.replace("\\r\\n", System.lineSeparator()).replace("\\n", System.lineSeparator())
    }

    if (tabsEncodingEnabled.get()) {
      text = text.replace("\\t", "\t")
    }

    if (backslashsEncodingEnabled.get()) {
      text = text.replace("\\\\", "\\")
    }

    if (singleQuotesEncodingEnabled.get()) {
      text = text.replace("\\'", "'")
    }

    if (doubleQuotesEncodingEnabled.get()) {
      text = text.replace("\\\"", "\"")
    }

    return text.toByteArray()
  }

  override fun Panel.buildBottomConfigurationUi() {
    collapsibleGroup(
      title = UiToolsBundle.message("escape-sequences-escaper-unescaper.settings.title")
    ) {
      groupRowsRange(
        UiToolsBundle.message(
          "escape-sequences-escaper-unescaper.settings.escape-sequences-to-be-decoded"
        )
      ) {
        row {
            checkBox(
                UiToolsBundle.message("escape-sequences-escaper-unescaper.do-line-break-decoding")
              )
              .bindSelected(lineBreaksEncodingEnabled)
              .gap(RightGap.SMALL)
            comboBox(LineBreak.entries)
              .bindItem(lineBreaksEncodingEscapeSequence)
              .enabledIf(lineBreaksEncodingEnabled)
          }
          .layout(RowLayout.PARENT_GRID)

        row {
            checkBox(UiToolsBundle.message("escape-sequences-escaper-unescaper.do-tab-decoding"))
              .bindSelected(tabsEncodingEnabled)
              .gap(RightGap.SMALL)

            checkBox(
                UiToolsBundle.message("escape-sequences-escaper-unescaper.do-backslash-decoding")
              )
              .bindSelected(backslashsEncodingEnabled)
              .gap(RightGap.SMALL)
          }
          .layout(RowLayout.PARENT_GRID)

        row {
            checkBox(
                UiToolsBundle.message("escape-sequences-escaper-unescaper.do-single-quote-decoding")
              )
              .bindSelected(singleQuotesEncodingEnabled)
              .gap(RightGap.SMALL)

            checkBox(
                UiToolsBundle.message("escape-sequences-escaper-unescaper.do-double-quote-decoding")
              )
              .bindSelected(doubleQuotesEncodingEnabled)
              .gap(RightGap.SMALL)
          }
          .layout(RowLayout.PARENT_GRID)
      }
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

  class Factory : DeveloperUiToolFactory<EscapeSequencesEscaperUnescaper> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("escape-sequences-escaper-unescaper.title"),
        contentTitle = UiToolsBundle.message("escape-sequences-escaper-unescaper.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> EscapeSequencesEscaperUnescaper) = { configuration ->
      EscapeSequencesEscaperUnescaper(configuration, parentDisposable, context, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
