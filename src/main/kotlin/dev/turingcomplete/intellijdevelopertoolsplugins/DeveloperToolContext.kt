package dev.turingcomplete.intellijdevelopertoolsplugins

import org.jetbrains.annotations.Nls

data class DeveloperToolContext(
        @Nls(capitalization = Nls.Capitalization.Title)
        val menuTitle: String,

        @Nls(capitalization = Nls.Capitalization.Title)
        val contentTitle: String
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}