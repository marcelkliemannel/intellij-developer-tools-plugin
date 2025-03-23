package dev.turingcomplete.intellijdevelopertoolsplugin.common

import dev.turingcomplete.textcaseconverter.StandardTextCases
import dev.turingcomplete.textcaseconverter.StandardWordsSplitters
import dev.turingcomplete.textcaseconverter.TextCase
import dev.turingcomplete.textcaseconverter.WordsSplitter

object TextCaseUtils {
  // -- Variables ----------------------------------------------------------- //

  private val spacesPattern = Regex("\\s+")

  private val textCasesWithoutSplitWords = setOf(
    StandardTextCases.LOWER_CASE,
    StandardTextCases.UPPER_CASE,
    StandardTextCases.INVERTED_CASE,
    StandardTextCases.ALTERNATING_CASE
  )

  val allTextCases = StandardTextCases.ALL_STANDARD_TEXT_CASES
    .sortedWith(sortTextCases())

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun determineWordsSplitter(text: String, targetTextCase: TextCase): WordsSplitter {
    if (textCasesWithoutSplitWords.contains(targetTextCase)) {
      return StandardWordsSplitters.NOOP
    }

    return when {
      spacesPattern.containsMatchIn(text) -> StandardWordsSplitters.SPACES
      text.contains("-") -> StandardWordsSplitters.DASH
      text.contains("_") -> StandardWordsSplitters.UNDERSCORE
      else -> StandardWordsSplitters.SOFT_UPPER_CASE
    }
  }

  // -- Private Methods ----------------------------------------------------- //

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

  // -- Inner Type ---------------------------------------------------------- //
}
