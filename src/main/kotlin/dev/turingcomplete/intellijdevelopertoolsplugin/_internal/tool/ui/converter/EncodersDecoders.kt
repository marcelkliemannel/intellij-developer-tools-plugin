package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.decodeFromAscii
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.encodeToAscii
import dev.turingcomplete.intellijdevelopertoolsplugin.i18n.I18nUtils
import org.apache.commons.codec.binary.Base32
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

// -- Properties ---------------------------------------------------------------------------------------------------- //

internal val encoderDecoderTextConverterContext = TextConverter.TextConverterContext(
  convertActionTitle = I18nUtils.message("DecoderTextConverterContext.convertActionTitle"),
  revertActionTitle = I18nUtils.message("DecoderTextConverterContext.revertActionTitle"),
  sourceTitle = I18nUtils.message("DecoderTextConverterContext.sourceTitle"),
  targetTitle = I18nUtils.message("DecoderTextConverterContext.targetTitle")
)

// -- Exposed Methods ----------------------------------------------------------------------------------------------- //
// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class Base32EncoderDecoder(
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
      menuTitle = I18nUtils.message("Base32EncoderDecoder.menuTitle"),
      groupedMenuTitle = I18nUtils.message("Base32EncoderDecoder.menuTitle"),
      contentTitle = I18nUtils.message("Base32EncoderDecoder.contentTitle")
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

internal class Base64EncoderDecoder(
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
      menuTitle = I18nUtils.message("Base64EncoderDecoder.menuTitle"),
      groupedMenuTitle = I18nUtils.message("Base64EncoderDecoder.menuTitle"),
      contentTitle = I18nUtils.message("Base64EncoderDecoder.contentTitle")
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

internal class UrlBase64EncoderDecoder(
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
      menuTitle = I18nUtils.message("UrlBase64EncoderDecoder.menuTitle"),
      groupedMenuTitle = I18nUtils.message("UrlBase64EncoderDecoder.menuTitle"),
      contentTitle = I18nUtils.message("UrlBase64EncoderDecoder.contentTitle")
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

internal class MimeBase64EncoderDecoder(
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
      menuTitle = I18nUtils.message("MimeBase64EncoderDecoder.menuTitle"),
      groupedMenuTitle = I18nUtils.message("MimeBase64EncoderDecoder.menuTitle"),
      contentTitle = I18nUtils.message("MimeBase64EncoderDecoder.contentTitle")
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

internal class AsciiEncoderDecoder(
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
      menuTitle = I18nUtils.message("AsciiEncoderDecoder.menuTitle"),
      groupedMenuTitle = I18nUtils.message("AsciiEncoderDecoder.menuTitle"),
      contentTitle = I18nUtils.message("AsciiEncoderDecoder.contentTitle")
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

internal class UrlEncodingEncoderDecoder(
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
      menuTitle = I18nUtils.message("UrlEncodingEncoderDecoder.menuTitle"),
      groupedMenuTitle = I18nUtils.message("UrlEncodingEncoderDecoder.menuTitle"),
      contentTitle = I18nUtils.message("UrlEncodingEncoderDecoder.contentTitle")
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
