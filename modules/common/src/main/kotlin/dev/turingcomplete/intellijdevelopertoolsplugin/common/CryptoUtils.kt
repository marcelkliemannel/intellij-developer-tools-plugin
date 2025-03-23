package dev.turingcomplete.intellijdevelopertoolsplugin.common

import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

object CryptoUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val RAW_KEY_REGEX = Regex("\\r?\\n|\\r|\\s?-+(BEGIN|END).*KEY-+\\s?")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun toRkcs8Key(keyInput: String): PKCS8EncodedKeySpec =
    PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyInput.replace(RAW_KEY_REGEX, "")))


  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
