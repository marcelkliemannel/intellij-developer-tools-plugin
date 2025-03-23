package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.openapi.ui.InputValidator

class NotBlankInputValidator : InputValidator {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun checkInput(inputString: String?): Boolean =
    inputString?.isNotBlank() ?: false

  override fun canClose(inputString: String?): Boolean =
    inputString?.isNotBlank() ?: false

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
