@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.GeneralDeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.transformer.TextCaseTransformer.OriginalParsingMode.FIXED_TEXT_CASE
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.transformer.TextCaseTransformer.OriginalParsingMode.INDIVIDUAL_DELIMITER
import dev.turingcomplete.intellijdevelopertoolsplugins.onChanged
import dev.turingcomplete.intellijdevelopertoolsplugins.onSelected
import dev.turingcomplete.textcaseconverter.StandardTextCases
import dev.turingcomplete.textcaseconverter.toTextCase
import dev.turingcomplete.textcaseconverter.toWordsSplitter
import dev.turingcomplete.textcaseconverter.TextCase as StandardTextCase

class TextCaseTransformer :
  TextTransformer(
        id = "text-case",
        title = "Text Case",
        transformActionTitle = "Transform",
        sourceTitle = "Original",
        resultTitle = "Target"
  ),
  GeneralDeveloperTool {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var originalParsingMode by createProperty("originalParsingMode", FIXED_TEXT_CASE)
  private var individualDelimiter by createProperty("individualDelimiter", " ")
  private var inputTextCase by createProperty("inputTextCase", TextCase.CAMEL_CASE)
  private var outputTextCase by createProperty("outputTextCase", TextCase.COBOL_CASE)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform(text: String): String = when(originalParsingMode) {
    FIXED_TEXT_CASE -> text.toTextCase(outputTextCase.textCase, inputTextCase.textCase.wordsSplitter())
    INDIVIDUAL_DELIMITER -> text.toTextCase(outputTextCase.textCase, individualDelimiter.toWordsSplitter())
  }

  override fun Panel.buildConfigurationUi(project: Project?, parentDisposable: Disposable) {
    buttonsGroup("Original:") {
      row {
        radioButton("Fixed text case:").configure(FIXED_TEXT_CASE).gap(RightGap.SMALL)
        comboBox(TextCase.values().toList()).configure(inputTextCase) { inputTextCase = it }
      }
      row {
        val individualDelimiterRadioButton = radioButton("Split words by:").configure(INDIVIDUAL_DELIMITER).gap(RightGap.SMALL)
        textField().text(individualDelimiter)
                .whenTextChangedFromUi(parentDisposable) { individualDelimiter = it }
                .enabledIf(individualDelimiterRadioButton.selected).component
      }
    }

    row {
      comboBox(TextCase.values().toList()).label("Target:").configure(outputTextCase) { outputTextCase = it }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Cell<ComboBox<TextCase>>.configure(initialTextCase: TextCase, setTextCase: (TextCase) -> Unit) =
    applyToComponent {
      selectedItem = initialTextCase
      onChanged { setTextCase(it) }
    }

  private fun Cell<JBRadioButton>.configure(value: OriginalParsingMode) = this.applyToComponent {
    isSelected = originalParsingMode == value
    onSelected { originalParsingMode = value }
  }

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

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}