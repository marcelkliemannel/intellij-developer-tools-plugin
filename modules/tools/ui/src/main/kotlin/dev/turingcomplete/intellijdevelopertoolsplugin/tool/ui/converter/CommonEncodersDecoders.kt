package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.common.decodeFromAscii
import dev.turingcomplete.intellijdevelopertoolsplugin.common.encodeToAscii
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.apache.commons.codec.binary.Base32

// -- Properties ---------------------------------------------------------- //
// -- Exported Methods ---------------------------------------------------- //
// -- Private Methods  ---------------------------------------------------- //
// -- Inner Type ---------------------------------------------------------- //

class Base32EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("base32-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray = Base32().encode(source)

  override fun doConvertToSource(target: ByteArray): ByteArray = Base32().decode(target)

  class Factory : DeveloperUiToolFactory<Base32EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("base32-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("base32-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("base32-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> Base32EncoderDecoder) = { configuration ->
      Base32EncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class Base64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("base64-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray = Base64.getEncoder().encode(source)

  override fun doConvertToSource(target: ByteArray): ByteArray = Base64.getDecoder().decode(target)

  class Factory : DeveloperUiToolFactory<Base64EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("base64-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("base64-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("base64-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> Base64EncoderDecoder) = { configuration ->
      Base64EncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class UrlBase64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("url-base64-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    Base64.getUrlEncoder().encode(source)

  override fun doConvertToSource(target: ByteArray): ByteArray =
    Base64.getUrlDecoder().decode(target)

  class Factory : DeveloperUiToolFactory<UrlBase64EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("url-base64-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("url-base64-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("url-base64-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> UrlBase64EncoderDecoder) = { configuration ->
      UrlBase64EncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class MimeBase64EncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("mime-base64-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    Base64.getMimeEncoder().encode(source)

  override fun doConvertToSource(target: ByteArray): ByteArray =
    Base64.getMimeDecoder().decode(target)

  class Factory : DeveloperUiToolFactory<MimeBase64EncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("mime-base64-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("mime-base64-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("mime-base64-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> MimeBase64EncoderDecoder) = { configuration ->
      MimeBase64EncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class AsciiEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("ascii-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray = source.encodeToAscii()

  override fun doConvertToSource(target: ByteArray): ByteArray = target.decodeFromAscii()

  class Factory : DeveloperUiToolFactory<AsciiEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("ascii-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("ascii-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("ascii-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> AsciiEncoderDecoder) = { configuration ->
      AsciiEncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class UrlEncodingEncoderDecoder(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EncoderDecoder(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("url-encoding.title"),
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    URLEncoder.encode(String(source), StandardCharsets.UTF_8.name()).toByteArray()

  override fun doConvertToSource(target: ByteArray): ByteArray =
    URLDecoder.decode(String(target), StandardCharsets.UTF_8.name()).toByteArray()

  class Factory : DeveloperUiToolFactory<UrlEncodingEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("url-encoding.menu-title"),
        groupedMenuTitle = UiToolsBundle.message("url-encoding.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("url-encoding.title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> UrlEncodingEncoderDecoder) = { configuration ->
      UrlEncodingEncoderDecoder(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //
