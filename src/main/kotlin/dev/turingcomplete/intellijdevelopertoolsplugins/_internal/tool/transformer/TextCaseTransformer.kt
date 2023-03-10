package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.onSelected
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextCaseTransformer.OriginalParsingMode.FIXED_TEXT_CASE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextCaseTransformer.OriginalParsingMode.INDIVIDUAL_DELIMITER
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextCaseTransformer.TextCase.CAMEL_CASE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextCaseTransformer.TextCase.COBOL_CASE
import dev.turingcomplete.textcaseconverter.StandardTextCases
import dev.turingcomplete.textcaseconverter.toTextCase
import dev.turingcomplete.textcaseconverter.toWordsSplitter
import dev.turingcomplete.textcaseconverter.TextCase as StandardTextCase

class TextCaseTransformer(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextTransformer(
    presentation = DeveloperToolPresentation("Text Case", "Text Case Transformer"),
    transformActionTitle = "Transform",
    sourceTitle = "Original",
    resultTitle = "Target",
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var originalParsingMode by configuration.register("originalParsingMode", FIXED_TEXT_CASE)
  private var individualDelimiter by configuration.register("individualDelimiter", " ")
  private var inputTextCase by configuration.register("inputTextCase", CAMEL_CASE)
  private var outputTextCase by configuration.register("outputTextCase", COBOL_CASE)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform() {
    resultText = when (originalParsingMode) {
      FIXED_TEXT_CASE -> sourceText.toTextCase(outputTextCase.textCase, inputTextCase.textCase.wordsSplitter())
      INDIVIDUAL_DELIMITER -> sourceText.toTextCase(outputTextCase.textCase, individualDelimiter.toWordsSplitter())
    }
  }

  @Suppress("UnstableApiUsage")
  override fun Panel.buildMiddleConfigurationUi() {
    buttonsGroup("Original:") {
      row {
        radioButton("Fixed text case:").applyToComponent {
          isSelected = originalParsingMode == FIXED_TEXT_CASE
          onSelected { originalParsingMode = FIXED_TEXT_CASE }
        }.gap(RightGap.SMALL)
        comboBox(TextCase.values().toList())
                .applyToComponent { selectedItem = inputTextCase }
                .whenItemSelectedFromUi { inputTextCase = it }
      }

      row {
        val individualDelimiterRadioButton = radioButton("Split words by:")
                .applyToComponent {
                  isSelected = originalParsingMode == INDIVIDUAL_DELIMITER
                  onSelected { originalParsingMode = INDIVIDUAL_DELIMITER }
                }.gap(RightGap.SMALL)
        textField().text(individualDelimiter)
                .whenTextChangedFromUi(parentDisposable) { individualDelimiter = it }
                .enabledIf(individualDelimiterRadioButton.selected).component
      }
    }

    row {
      comboBox(TextCase.values().toList())
              .label("Target:")
              .applyToComponent { selectedItem = outputTextCase }
              .whenItemSelectedFromUi { outputTextCase = it }
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

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return TextCaseTransformer(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}