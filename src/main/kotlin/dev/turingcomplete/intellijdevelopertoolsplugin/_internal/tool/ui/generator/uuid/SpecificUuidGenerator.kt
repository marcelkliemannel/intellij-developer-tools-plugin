package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.generator.uuid

import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.layout.ComponentPredicate

abstract class SpecificUuidGenerator(val supportsBulkGeneration: Boolean) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun generate(): String

  open fun Panel.buildConfigurationUi(visible: ComponentPredicate) {
    // Override if needed
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}