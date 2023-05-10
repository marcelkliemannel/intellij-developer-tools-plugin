package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import org.apache.commons.codec.binary.Base32
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

// -- Properties ---------------------------------------------------------------------------------------------------- //

internal val encoderDecoderTextConverterContext = TextConverter.TextConverterContext(
  convertActionTitle = "Encode",
  revertActionTitle = "Decode",
  sourceTitle = "Encoded",
  targetTitle = "Decoded"
)

// -- Exposed Methods ----------------------------------------------------------------------------------------------- //
// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class Base32EncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(Base32().encodeToString(text.encodeToByteArray()))
  }

  override fun toSource(text: String) {
    sourceText.set(Base32().decode(text).decodeToString())
  }

  class Factory : DeveloperToolFactory<Base32EncoderDecoder> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "Base32",
      contentTitle = "Base32 Encoder/Decoder"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> Base32EncoderDecoder) =
      { configuration -> Base32EncoderDecoder(configuration, parentDisposable) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class Base64EncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(Base64.getEncoder().encodeToString(text.encodeToByteArray()))
  }

  override fun toSource(text: String) {
    sourceText.set(Base64.getDecoder().decode(text).decodeToString())
  }

  class Factory : DeveloperToolFactory<Base64EncoderDecoder> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "Base64",
      contentTitle = "Base64 Encoder/Decoder"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> Base64EncoderDecoder) =
      { configuration -> Base64EncoderDecoder(configuration, parentDisposable) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class UrlBase64EncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(Base64.getUrlEncoder().encodeToString(text.encodeToByteArray()))
  }

  override fun toSource(text: String) {
    sourceText.set(Base64.getUrlDecoder().decode(text).decodeToString())
  }

  class Factory : DeveloperToolFactory<UrlBase64EncoderDecoder> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "URL Base64",
      contentTitle = "URL Base64 Encoder/Decoder"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> UrlBase64EncoderDecoder) =
      { configuration -> UrlBase64EncoderDecoder(configuration, parentDisposable) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class MimeBase64EncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(Base64.getMimeEncoder().encodeToString(text.encodeToByteArray()))
  }

  override fun toSource(text: String) {
    sourceText.set(Base64.getMimeDecoder().decode(text).decodeToString())
  }

  class Factory : DeveloperToolFactory<MimeBase64EncoderDecoder> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "MIME Base64",
      contentTitle = "MIME Base64 Encoder/Decoder"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> MimeBase64EncoderDecoder) =
      { configuration -> MimeBase64EncoderDecoder(configuration, parentDisposable) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class UrlEncodingEncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(URLEncoder.encode(text, StandardCharsets.UTF_8))
  }

  override fun toSource(text: String) {
    sourceText.set(URLDecoder.decode(text, StandardCharsets.UTF_8))
  }

  class Factory : DeveloperToolFactory<UrlEncodingEncoderDecoder> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "URL Encoding",
      contentTitle = "URL Encoding Encoder/Decoder"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> UrlEncodingEncoderDecoder) =
      { configuration -> UrlEncodingEncoderDecoder(configuration, parentDisposable) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //
