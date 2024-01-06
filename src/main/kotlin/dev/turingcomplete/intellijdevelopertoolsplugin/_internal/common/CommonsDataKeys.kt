package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.openapi.actionSystem.DataKey

object CommonsDataKeys {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val SELECTED_VALUES: DataKey<List<String>> = DataKey.create("DeveloperToolsPlugin.selectedValues")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}