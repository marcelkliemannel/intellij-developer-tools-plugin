package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.selected
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextCaseUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.common.bind
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.OriginalParsingMode.AUTOMATIC_DETECTION
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.OriginalParsingMode.FIXED_TEXT_CASE
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.OriginalParsingMode.INDIVIDUAL_DELIMITER
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.TextCase.COBOL_CASE
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextCaseTransformer.TextCase.STRICT_CAMEL_CASE
import dev.turingcomplete.textcaseconverter.StandardTextCases
import dev.turingcomplete.textcaseconverter.toTextCase
import dev.turingcomplete.textcaseconverter.toWordsSplitter
import dev.turingcomplete.textcaseconverter.TextCase as StandardTextCase

class TextCaseTransformer(
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  project: Project?,
) :
  TextTransformer(
    textTransformerContext =
      TextTransformerContext(
        transformActionTitle = "Transform",
        sourceTitle = "Original",
        resultTitle = "Target",
        initialSourceExampleText = EXAMPLE_INPUT_TEXT,
        diffSupport = DiffSupport(title = "Text Case Transformer"),
        supportsDebug = true,
      ),
    context = context,
    configuration = configuration,
    parentDisposable = parentDisposable,
    project = project,
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var originalParsingMode =
    configuration.register("originalParsingMode", AUTOMATIC_DETECTION)
  private var individualDelimiter = configuration.register("individualDelimiter", " ")
  private var inputTextCase = configuration.register("inputTextCase", STRICT_CAMEL_CASE)
  private var outputTextCase = configuration.register("outputTextCase", COBOL_CASE)

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun transform() {
    resultText.set(
      sourceText.get().toTextCase(outputTextCase.get().textCase, getInputWordsSplitter())
    )
  }

  override fun Panel.buildMiddleConfigurationUi() {
    buttonsGroup("Original:") {
      row { radioButton("Automatic detection").bind(originalParsingMode, AUTOMATIC_DETECTION) }

      row {
        val fixedTextCaseRadioButton =
          radioButton("Fixed text case:")
            .bind(originalParsingMode, FIXED_TEXT_CASE)
            .gap(RightGap.SMALL)
        comboBox(TextCase.entries)
          .bindItem(inputTextCase)
          .enabledIf(fixedTextCaseRadioButton.selected)
          .component
      }

      row {
        val individualDelimiterRadioButton =
          radioButton("Split words by:")
            .bind(originalParsingMode, INDIVIDUAL_DELIMITER)
            .gap(RightGap.SMALL)
        textField()
          .bindText(individualDelimiter)
          .enabledIf(individualDelimiterRadioButton.selected)
          .component
      }
    }

    row { comboBox(TextCase.entries).label("Target:").bindItem(outputTextCase) }
  }

  override fun Panel.buildDebugComponent() {
    group("Input Words") {
      row {
        val inputWords = getInputWordsSplitter().split(sourceText.get())
        if (inputWords.isEmpty()) {
          label("<html><i>None</i></html>")
        } else {
          val wordsList =
            inputWords.joinToString(separator = "", prefix = "<ol>", postfix = "</ol>") {
              "<li>$it</li>"
            }
          label("<html>$wordsList</html>")
        }
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun getInputWordsSplitter() =
    when (originalParsingMode.get()) {
      AUTOMATIC_DETECTION ->
        TextCaseUtils.determineWordsSplitter(sourceText.get(), inputTextCase.get().textCase)
      FIXED_TEXT_CASE -> inputTextCase.get().textCase.wordsSplitter()
      INDIVIDUAL_DELIMITER -> individualDelimiter.get().toWordsSplitter()
    }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class TextCase(val textCase: StandardTextCase) {

    SCREAMING_SNAKE_CASE(StandardTextCases.SCREAMING_SNAKE_CASE),
    SOFT_CAMEL_CASE(StandardTextCases.SOFT_CAMEL_CASE),
    STRICT_CAMEL_CASE(StandardTextCases.STRICT_CAMEL_CASE),
    PASCAL_CASE(StandardTextCases.PASCAL_CASE),
    SNAKE_CASE(StandardTextCases.SNAKE_CASE),
    KEBAB_CASE(StandardTextCases.KEBAB_CASE),
    TRAIN_CASE(StandardTextCases.TRAIN_CASE),
    COBOL_CASE(StandardTextCases.COBOL_CASE),
    PASCAL_SNAKE_CASE(StandardTextCases.PASCAL_SNAKE_CASE),
    CAMEL_SNAKE_CASE(StandardTextCases.CAMEL_SNAKE_CASE),
    LOWER_CASE(StandardTextCases.LOWER_CASE),
    UPPER_CASE(StandardTextCases.UPPER_CASE),
    INVERTED_CASE(StandardTextCases.INVERTED_CASE),
    ALTERNATING_CASE(StandardTextCases.ALTERNATING_CASE),
    DOT_CASE(StandardTextCases.DOT_CASE);

    override fun toString(): String = "${textCase.title()} (${textCase.example()})"
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class OriginalParsingMode {

    AUTOMATIC_DETECTION,
    FIXED_TEXT_CASE,
    INDIVIDUAL_DELIMITER,
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextCaseTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "Text Case", contentTitle = "Text Case Transformer")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> TextCaseTransformer) = { configuration ->
      TextCaseTransformer(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    const val EXAMPLE_INPUT_TEXT = "thisIsAnExampleText"
  }
}
