package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.textescape

import com.intellij.openapi.extensions.ExtensionPointName
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.TextConverter

abstract class TextEscape(id: String, title: String)
  : TextConverter(id = id,
                  title = title,
                  convertActionTitle = "Escape",
                  revertActionTitle = "Unescape",
                  sourceTitle = "Unescaped",
                  targetTitle = "Escaped") {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val EP: ExtensionPointName<TextEscape> = ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.textEscape")
  }
}