package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.uuid

import com.intellij.openapi.extensions.ExtensionPointName
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.OneLineTextGenerator

abstract class UuidGenerator(
        title: String,
        description: String? = null,
        supportsBulkGeneration: Boolean = true
) : OneLineTextGenerator(id = title.lowercase(),
                         title = title,
                         description = description,
                         supportsBulkGeneration = supportsBulkGeneration) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val EP: ExtensionPointName<UuidGenerator> = ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.uuid")
  }
}