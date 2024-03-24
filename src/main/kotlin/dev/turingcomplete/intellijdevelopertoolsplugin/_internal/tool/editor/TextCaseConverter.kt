package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.TextCaseUtils.determineWordsSplitter
import dev.turingcomplete.textcaseconverter.StandardTextCases
import dev.turingcomplete.textcaseconverter.TextCase

internal object TextCaseConverter {
  // -- Variables --------------------------------------------------------------------------------------------------- //

  val allTextCases = StandardTextCases.ALL_STANDARD_TEXT_CASES
    .sortedWith(sortTextCases())

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun executeConversionInEditor(
    text: String,
    textRange: TextRange,
    textCase: TextCase,
    editor: Editor
  ) {
    val wordsSplitter = determineWordsSplitter(text, textCase)
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