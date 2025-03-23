package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.common.decodeFromAscii
import dev.turingcomplete.intellijdevelopertoolsplugin.common.encodeToAscii
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import org.apache.commons.codec.binary.Base32
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

// -- Properties ---------------------------------------------------------------------------------------------------- //

val encoderDecoderTextConverterContext = TextConverter.TextConverterContext(
  convertActionTitle = "Encode",
  revertActionTitle = "Decode",
  sourceTitle = "Decoded",
  targetTitle = "Encoded"
)

// -- Exposed Methods ----------------------------------------------------------------------------------------------- //
// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

class Base32EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(Base32().encodeToString(text.encodeToByteArray()))
  }

  override fun toSource(text: String) {
    sourceText.set(Base32().decode(text).decodeToString())
  }

  class Factory : DeveloperUiToolFactory<Base32EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Base32 Encoding",
      groupedMenuTitle = "Base32",
      contentTitle = "Base32 Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> Base32EncoderDecoder) =
      { configuration -> Base32EncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class Base64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(Base64.getEncoder().encodeToString(text.encodeToByteArray()))
  }

  override fun toSource(text: String) {
    sourceText.set(Base64.getDecoder().decode(text).decodeToString())
  }

  class Factory : DeveloperUiToolFactory<Base64EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Base64 Encoding",
      groupedMenuTitle = "Base64",
      contentTitle = "Base64 Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> Base64EncoderDecoder) =
      { configuration -> Base64EncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UrlBase64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(Base64.getUrlEncoder().encodeToString(text.encodeToByteArray()))
  }

  override fun toSource(text: String) {
    sourceText.set(Base64.getUrlDecoder().decode(text).decodeToString())
  }

  class Factory : DeveloperUiToolFactory<UrlBase64EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "URL Base64 Encoding",
      groupedMenuTitle = "URL Base64",
      contentTitle = "URL Base64 Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> UrlBase64EncoderDecoder) =
      { configuration -> UrlBase64EncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class MimeBase64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(Base64.getMimeEncoder().encodeToString(text.encodeToByteArray()))
  }

  override fun toSource(text: String) {
    sourceText.set(Base64.getMimeDecoder().decode(text).decodeToString())
  }

  class Factory : DeveloperUiToolFactory<MimeBase64EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "MIME Base64 Encoding",
      groupedMenuTitle = "MIME Base64",
      contentTitle = "MIME Base64 Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> MimeBase64EncoderDecoder) =
      { configuration -> MimeBase64EncoderDecoder(configuration, parentDisposable, context, project) }
  }
}
// -- Type ---------------------------------------------------------------------------------------------------------- //

class AsciiEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(text.encodeToAscii())
  }

  override fun toSource(text: String) {
    sourceText.set(text.decodeFromAscii())
  }

  class Factory : DeveloperUiToolFactory<AsciiEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "ASCII Encoding",
      groupedMenuTitle = "ASCII",
      contentTitle = "ASCII Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> AsciiEncoderDecoder) =
      { configuration -> AsciiEncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UrlEncodingEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = encoderDecoderTextConverterContext,
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(URLEncoder.encode(text, StandardCharsets.UTF_8))
  }

  override fun toSource(text: String) {
    sourceText.set(URLDecoder.decode(text, StandardCharsets.UTF_8))
  }

  class Factory : DeveloperUiToolFactory<UrlEncodingEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "URL Encoding",
      groupedMenuTitle = "URL",
      contentTitle = "URL Encoding Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> UrlEncodingEncoderDecoder) =
      { configuration -> UrlEncodingEncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //
