package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common

import com.intellij.openapi.extensions.ExtensionPointName

interface GeneralDeveloperTool {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val EP: ExtensionPointName<GeneralDeveloperTool> = ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.general")
  }
}