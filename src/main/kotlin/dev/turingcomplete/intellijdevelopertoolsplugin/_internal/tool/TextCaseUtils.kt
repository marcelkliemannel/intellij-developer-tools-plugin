package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool

import dev.turingcomplete.textcaseconverter.StandardTextCases
import dev.turingcomplete.textcaseconverter.StandardWordsSplitters
import dev.turingcomplete.textcaseconverter.TextCase
import dev.turingcomplete.textcaseconverter.WordsSplitter

object TextCaseUtils {
  // -- Variables --------------------------------------------------------------------------------------------------- //

  private val spacesPattern = Regex("\\s+")

  private val textCasesWithWithoutSplitWords = setOf(
    StandardTextCases.LOWER_CASE,
    StandardTextCases.UPPER_CASE,
    StandardTextCases.INVERTED_CASE,
    StandardTextCases.ALTERNATING_CASE
  )

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun determineWordsSplitter(text: String, targetTextCase: TextCase): WordsSplitter {
    if (textCasesWithWithoutSplitWords.contains(targetTextCase)) {
      return StandardWordsSplitters.NOOP
    }

    return when {
      spacesPattern.containsMatchIn(text) -> StandardWordsSplitters.SPACES
      text.contains("-") -> StandardWordsSplitters.DASH
      text.contains("_") -> StandardWordsSplitters.UNDERSCORE
      else -> StandardWordsSplitters.SOFT_UPPER_CASE
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}