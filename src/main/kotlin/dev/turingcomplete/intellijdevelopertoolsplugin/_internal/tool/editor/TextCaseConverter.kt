package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.textcaseconverter.StandardTextCases
import dev.turingcomplete.textcaseconverter.StandardTextCases.ALTERNATING_CASE
import dev.turingcomplete.textcaseconverter.StandardTextCases.INVERTED_CASE
import dev.turingcomplete.textcaseconverter.StandardTextCases.LOWER_CASE
import dev.turingcomplete.textcaseconverter.StandardTextCases.UPPER_CASE
import dev.turingcomplete.textcaseconverter.StandardWordsSplitters
import dev.turingcomplete.textcaseconverter.StandardWordsSplitters.NOOP
import dev.turingcomplete.textcaseconverter.TextCase
import dev.turingcomplete.textcaseconverter.WordsSplitter

internal object TextCaseConverter {
  // -- Variables --------------------------------------------------------------------------------------------------- //

  val allTextCases = StandardTextCases.ALL_STANDARD_TEXT_CASES
    .sortedWith(sortTextCases())

  private val spacesPattern = Regex("\\s+")

  val nonSplitWords = setOf(LOWER_CASE, UPPER_CASE, INVERTED_CASE, ALTERNATING_CASE)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun determineWordsSplitter(text: String): WordsSplitter = when {
      spacesPattern.containsMatchIn(text) -> StandardWordsSplitters.SPACES
      text.contains("-") -> StandardWordsSplitters.DASH
      text.contains("_") -> StandardWordsSplitters.UNDERSCORE
      else -> StandardWordsSplitters.SOFT_UPPER_CASE
    }

  fun executeConversionInEditor(
    text: String,
    textRange: TextRange,
    textCase: TextCase,
    editor: Editor
  ) {
    val wordsSplitter = if (nonSplitWords.contains(textCase)) NOOP else determineWordsSplitter(text)
    val result = textCase.convert(text, wordsSplitter)
    editor.executeWriteCommand("Convert text case to ${textCase.title().lowercase()}") {
      it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun sortTextCases() = compareBy<TextCase> {
    val primaryTextCases = listOf(
      StandardTextCases.SCREAMING_SNAKE_CASE,
      StandardTextCases.SOFT_CAMEL_CASE,
      StandardTextCases.STRICT_CAMEL_CASE,
      StandardTextCases.PASCAL_CASE,
      StandardTextCases.SNAKE_CASE,
      StandardTextCases.KEBAB_CASE
    )
    if (it in primaryTextCases) {
      primaryTextCases.indexOf(it)
    }
    else {
      primaryTextCases.size + StandardTextCases.ALL_STANDARD_TEXT_CASES.indexOf(it)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}