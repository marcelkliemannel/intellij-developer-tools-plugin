package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.encoderdecoder

import org.bouncycastle.util.encoders.Base32
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.UrlBase64
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //
// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

class Base32EncoderDecoder : EncoderDecoder("base32", "Base32") {

  override fun toTarget(text: String): String = Base32.toBase32String(text.encodeToByteArray())

  override fun toSource(text: String): String = Base32.decode(text).decodeToString()
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class Base64EncoderDecoder : EncoderDecoder("base64", "Base64") {

  override fun toTarget(text: String): String = Base64.toBase64String(text.encodeToByteArray())

  override fun toSource(text: String): String = Base64.decode(text).decodeToString()
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UrlBase64EncoderDecoder : EncoderDecoder("url-base64", "URL Base64") {

  override fun toTarget(text: String): String = UrlBase64.encode(text.encodeToByteArray()).decodeToString()

  override fun toSource(text: String): String = UrlBase64.decode(text).decodeToString()
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UrlEncodingEncoderDecoder : EncoderDecoder("url-encoding", "URL Encoding") {

  override fun toTarget(text: String): String = URLEncoder.encode(text, StandardCharsets.UTF_8)

  override fun toSource(text: String): String = URLDecoder.decode(text, StandardCharsets.UTF_8)
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class TabsEncoderDecoder : EncoderDecoder("tabs", "Tabs") {

  override fun toTarget(text: String): String = text.replace("\t", "\\t")

  override fun toSource(text: String): String = text.replace("\\t", "\t")
}
