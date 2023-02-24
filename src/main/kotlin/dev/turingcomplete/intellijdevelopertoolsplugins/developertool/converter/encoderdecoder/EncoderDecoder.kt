package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.encoderdecoder

import com.intellij.openapi.extensions.ExtensionPointName
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.TextConverter

abstract class EncoderDecoder(id: String, title: String, description: String? = null) :
  TextConverter(id = id,
                title = title,
                convertActionTitle = "Encode",
                revertActionTitle = "Decode",
                sourceTitle = "Encoded",
                targetTitle = "Decoded",
                description = description) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val EP: ExtensionPointName<EncoderDecoder> = ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.encoderDecoder")
  }
}