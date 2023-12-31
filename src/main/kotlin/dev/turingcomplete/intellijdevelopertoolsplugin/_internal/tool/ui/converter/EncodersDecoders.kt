package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolExContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import org.apache.commons.codec.binary.Base32
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

// -- Properties ---------------------------------------------------------------------------------------------------- //

internal val encoderDecoderTextConverterContext = TextConverter.TextConverterContext(
  convertActionTitle = "Encode",
  revertActionTitle = "Decode",
  sourceTitle = "Decoded",
  targetTitle = "Encoded"
)

// -- Exposed Methods ----------------------------------------------------------------------------------------------- //
// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class Base32EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolExContext,
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
      menuTitle = "Base32",
      contentTitle = "Base32 Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolExContext
    ): ((DeveloperToolConfiguration) -> Base32EncoderDecoder) =
      { configuration -> Base32EncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class Base64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolExContext,
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
      menuTitle = "Base64",
      contentTitle = "Base64 Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolExContext
    ): ((DeveloperToolConfiguration) -> Base64EncoderDecoder) =
      { configuration -> Base64EncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class UrlBase64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolExContext,
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
      menuTitle = "URL Base64",
      contentTitle = "URL Base64 Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolExContext
    ): ((DeveloperToolConfiguration) -> UrlBase64EncoderDecoder) =
      { configuration -> UrlBase64EncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class MimeBase64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolExContext,
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
      menuTitle = "MIME Base64",
      contentTitle = "MIME Base64 Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolExContext
    ): ((DeveloperToolConfiguration) -> MimeBase64EncoderDecoder) =
      { configuration -> MimeBase64EncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class UrlEncodingEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolExContext,
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
      contentTitle = "URL Encoding Encoder/Decoder"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolExContext
    ): ((DeveloperToolConfiguration) -> UrlEncodingEncoderDecoder) =
      { configuration -> UrlEncodingEncoderDecoder(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //
