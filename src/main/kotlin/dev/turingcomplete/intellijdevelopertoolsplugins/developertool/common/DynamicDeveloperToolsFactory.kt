package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common

import com.intellij.openapi.extensions.ExtensionPointName
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool

abstract class DynamicDeveloperToolsFactory<T : DeveloperTool>(val title: String, val requiresProject: Boolean = false) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val developerTools: List<T> by lazy { createDeveloperTools() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun createDeveloperTools(): List<T>

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val EP: ExtensionPointName<DynamicDeveloperToolsFactory<*>> = ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.dynamic")
  }
}