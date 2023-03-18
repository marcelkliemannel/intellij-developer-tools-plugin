package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import org.bouncycastle.util.encoders.Base32
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

// -- Properties ---------------------------------------------------------------------------------------------------- //

internal val encoderDecoderContext = TextConverter.Context(
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
    developerToolContext = DeveloperToolContext("Base32", "Base32 Encoder/Decoder"),
    context = encoderDecoderContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = Base32.toBase32String(text.encodeToByteArray())

  override fun toSource(text: String): String = Base32.decode(text).decodeToString()

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return Base32EncoderDecoder(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class Base64EncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    developerToolContext = DeveloperToolContext("Base64", "Base64 Encoder/Decoder"),
    context = encoderDecoderContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = Base64.getEncoder().encodeToString(text.encodeToByteArray())

  override fun toSource(text: String): String = Base64.getDecoder().decode(text).decodeToString()

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return Base64EncoderDecoder(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class UrlBase64EncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    developerToolContext = DeveloperToolContext("URL Base64", "URL Base64 Encoder/Decoder"),
    context = encoderDecoderContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = Base64.getUrlEncoder().encodeToString(text.encodeToByteArray())

  override fun toSource(text: String): String = Base64.getUrlDecoder().decode(text).decodeToString()

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return UrlBase64EncoderDecoder(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class MimeBase64EncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    developerToolContext = DeveloperToolContext(menuTitle = "MIME Base64", contentTitle = "MIME Base64 Encoder/Decoder"),
    context = encoderDecoderContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = Base64.getMimeEncoder().encodeToString(text.encodeToByteArray())

  override fun toSource(text: String): String = Base64.getMimeDecoder().decode(text).decodeToString()

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return MimeBase64EncoderDecoder(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class UrlEncodingEncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    developerToolContext = DeveloperToolContext("URL Encoding", "URL Encoding Encoder/Decoder"),
    context = encoderDecoderContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = URLEncoder.encode(text, StandardCharsets.UTF_8)

  override fun toSource(text: String): String = URLDecoder.decode(text, StandardCharsets.UTF_8)

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return UrlEncodingEncoderDecoder(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //
