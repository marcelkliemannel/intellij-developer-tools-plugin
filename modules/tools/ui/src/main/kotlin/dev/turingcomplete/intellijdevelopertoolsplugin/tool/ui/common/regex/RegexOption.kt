package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.regex

import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.GeneralBundle
import java.util.regex.Pattern

enum class RegexOption(val patternFlag: Int, val title: String, val description: String? = null) {
  // -- Values -------------------------------------------------------------- //

  CASE_INSENSITIVE(
    Pattern.CASE_INSENSITIVE,
    GeneralBundle.message("regex-options.case-insensitive.title"),
    GeneralBundle.message("regex-options.case-insensitive.description"),
  ),
  UNICODE_CASE(
    Pattern.UNICODE_CASE,
    GeneralBundle.message("regex-options.unicode-case.title"),
    GeneralBundle.message("regex-options.unicode-case.description"),
  ),
  MULTILINE(
    Pattern.MULTILINE,
    GeneralBundle.message("regex-options.multiline.title"),
    GeneralBundle.message("regex-options.multiline.description"),
  ),
  DOTALL(
    Pattern.DOTALL,
    GeneralBundle.message("regex-options.dotall.title"),
    GeneralBundle.message("regex-options.dotall.description"),
  ),
  CANON_EQ(
    Pattern.CANON_EQ,
    GeneralBundle.message("regex-options.canon-eq.title"),
    GeneralBundle.message("regex-options.canon-eq.description"),
  ),
  UNIX_LINES(
    Pattern.UNIX_LINES,
    GeneralBundle.message("regex-options.unix-lines.title"),
    GeneralBundle.message("regex-options.unix-lines.description"),
  ),
  LITERAL(
    Pattern.LITERAL,
    GeneralBundle.message("regex-options.literal.title"),
    GeneralBundle.message("regex-options.literal.description"),
  ),
  COMMENTS(
    Pattern.COMMENTS,
    GeneralBundle.message("regex-options.comments.title"),
    GeneralBundle.message("regex-options.comments.description"),
  );

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun isSelected(regexOptionFlag: Int) = regexOptionFlag.and(patternFlag) != 0

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
