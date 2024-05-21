package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.regex

import java.util.regex.Pattern

enum class RegexOption(val patternFlag: Int, val title: String, val description: String? = null) {
  // -- Values ------------------------------------------------------------------------------------------------------ //

  CASE_INSENSITIVE(
    Pattern.CASE_INSENSITIVE,
    "Case-insensitive",
    "Case-insensitive matching will use characters for the US-ASCII charset for matching."
  ),
  UNICODE_CASE(
    Pattern.UNICODE_CASE,
    "Unicode-aware",
    "The <code>case insensitive</code> option will use the Unicode standard."
  ),
  MULTILINE(
    Pattern.MULTILINE,
    "Multiline",
    "The expressions <code>^</code> and <code>\$</code> match just after or just before, respectively, a line terminator or the end of the input sequence."
  ),
  DOTALL(
    Pattern.DOTALL,
    "Dotall",
    "The expression <code>.</code> will also match line terminators."
  ),
  CANON_EQ(
    Pattern.CANON_EQ,
    "Canonical equivalence",
    "Two characters will be considered to match if, and only if, their full canonical decompositions match."
  ),
  UNIX_LINES(
    Pattern.UNIX_LINES,
    "Unix line endings",
    "Only the <code>\\n</code> line terminator is recognized in the behavior of <code>.</code>, <code>^</code>, and <code>\$</code>."
  ),
  LITERAL(
    Pattern.LITERAL,
    "Literal parsing of the pattern",
    "The input string that specifies the pattern will be treated as a sequence of literal characters."
  ),
  COMMENTS(
    Pattern.COMMENTS,
    "Permit whitespace and comments in pattern",
    "Whitespace will be ignored, and embedded comments starting with <code>#</code> are ignored until the end of a line."
  );

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun isSelected(regexOptionFlag: Int) = regexOptionFlag.and(patternFlag) != 0

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}