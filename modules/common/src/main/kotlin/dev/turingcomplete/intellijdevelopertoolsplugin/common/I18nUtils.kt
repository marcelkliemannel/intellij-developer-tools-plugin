package dev.turingcomplete.intellijdevelopertoolsplugin.common

import dev.turingcomplete.intellijdevelopertoolsplugin.common.message.CommonBundle

object I18nUtils {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun formatLocalizedList(items: List<String>): String {
    return when (items.size) {
      0 -> ""
      1 -> items[0]
      2 -> items[0] + CommonBundle.message("list.conjunction.and") + items[1]
      else -> {
        val allButLast =
          items.dropLast(1).joinToString(CommonBundle.message("list.separator.comma"))
        "$allButLast${CommonBundle.message("list.conjunction.and")}${items.last()}"
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
