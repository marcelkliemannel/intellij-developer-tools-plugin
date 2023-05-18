package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.selected
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.bind
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextCaseTransformer.OriginalParsingMode.FIXED_TEXT_CASE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextCaseTransformer.OriginalParsingMode.INDIVIDUAL_DELIMITER
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextCaseTransformer.TextCase.CAMEL_CASE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextCaseTransformer.TextCase.COBOL_CASE
import dev.turingcomplete.textcaseconverter.StandardTextCases
import dev.turingcomplete.textcaseconverter.toTextCase
import dev.turingcomplete.textcaseconverter.toWordsSplitter
import dev.turingcomplete.textcaseconverter.TextCase as StandardTextCase

class TextCaseTransformer(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextTransformer(
  textTransformerContext = TextTransformerContext(
    transformActionTitle = "Transform",
    sourceTitle = "Original",
    resultTitle = "Target",
    diffSupport = DiffSupport(
      title = "Text Case Transformer"
    )
  ),
  configuration = configuration,
  parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var originalParsingMode = configuration.register("originalParsingMode", FIXED_TEXT_CASE)
  private var individualDelimiter = configuration.register("individualDelimiter", " ")
  private var inputTextCase = configuration.register("inputTextCase", CAMEL_CASE)
  private var outputTextCase = configuration.register("outputTextCase", COBOL_CASE)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform() {
    resultText.set(
      when (originalParsingMode.get()) {
        FIXED_TEXT_CASE -> sourceText.get().toTextCase(outputTextCase.get().textCase, inputTextCase.get().textCase.wordsSplitter())
        INDIVIDUAL_DELIMITER -> sourceText.get().toTextCase(outputTextCase.get().textCase, individualDelimiter.get().toWordsSplitter())
      }
    )
  }

  override fun Panel.buildMiddleConfigurationUi() {
    buttonsGroup("Original:") {
      row {
        radioButton("Fixed text case:")
          .bind(originalParsingMode, FIXED_TEXT_CASE)
          .gap(RightGap.SMALL)
        comboBox(TextCase.values().toList())
          .bindItem(inputTextCase)
      }

      row {
        val individualDelimiterRadioButton = radioButton("Split words by:")
          .bind(originalParsingMode, INDIVIDUAL_DELIMITER)
          .gap(RightGap.SMALL)
        textField()
          .bindText(individualDelimiter)
          .enabledIf(individualDelimiterRadioButton.selected).component
      }
    }

    row {
      comboBox(TextCase.values().toList())
        .label("Target:")
        .bindItem(outputTextCase)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class TextCase(val textCase: StandardTextCase) {

    CAMEL_CASE(StandardTextCases.CAMEL_CASE),
    KEBAB_CASE(StandardTextCases.KEBAB_CASE),
    SNAKE_CASE(StandardTextCases.SNAKE_CASE),
    TRAIN_CASE(StandardTextCases.TRAIN_CASE),
    COBOL_CASE(StandardTextCases.COBOL_CASE),
    SCREAMING_SNAKE_CASE(StandardTextCases.SCREAMING_SNAKE_CASE),
    PASCAL_CASE(StandardTextCases.PASCAL_CASE),
    PASCAL_SNAKE_CASE(StandardTextCases.PASCAL_SNAKE_CASE),
    CAMEL_SNAKE_CASE(StandardTextCases.CAMEL_SNAKE_CASE),
    LOWER_CASE(StandardTextCases.LOWER_CASE),
    UPPER_CASE(StandardTextCases.UPPER_CASE),
    INVERTED_CASE(StandardTextCases.INVERTED_CASE),
    ALTERNATING_CASE(StandardTextCases.ALTERNATING_CASE);

    override fun toString(): String = "${textCase.title()} (${textCase.example()})"
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class OriginalParsingMode {

    FIXED_TEXT_CASE,
    INDIVIDUAL_DELIMITER
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<TextCaseTransformer> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "Text Case",
      contentTitle = "Text Case Transformer"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> TextCaseTransformer) = { configuration ->
      TextCaseTransformer(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}