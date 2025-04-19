package dev.turingcomplete.intellijdevelopertoolsplugin.common

import java.security.Provider
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

object CryptoUtils {
  // -- Properties ---------------------------------------------------------- //

  private val RAW_KEY_REGEX = Regex("\\r?\\n|\\r|\\s?-+(BEGIN|END).*KEY-+\\s?")

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun toRkcs8Key(keyInput: String): PKCS8EncodedKeySpec =
    PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyInput.replace(RAW_KEY_REGEX, "")))

  fun registerBouncyCastleProvider() {
    val bouncyCastleProviderClass =
      Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
    val bouncyCastleProvider = bouncyCastleProviderClass.getConstructor().newInstance()
    Security.addProvider(bouncyCastleProvider as Provider)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
