package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import java.util.*

/**
 * A wrapper class for a [Locale]. This is required because the search function
 * of the `comboBox` in the UI DSL does work on the human-readable the display
 * name. It will use the `toString()` (e.g., `de_DE`) representation of the [Locale]
 * instead.
 */
data class LocaleContainer(val locale: Locale) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun toString(): String = locale.displayName

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}