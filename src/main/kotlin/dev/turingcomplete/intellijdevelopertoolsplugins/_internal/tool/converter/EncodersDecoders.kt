package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import org.bouncycastle.util.encoders.Base32
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.UrlBase64
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// -- Properties ---------------------------------------------------------------------------------------------------- //

internal const val GROUP = "Encoder/Decoder"

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
    presentation = DeveloperToolPresentation("Base32", "Base32 Encoder/Decoder"),
    context = encoderDecoderContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = Base32.toBase32String(text.encodeToByteArray())

  override fun toSource(text: String): String = Base32.decode(text).decodeToString()

  override fun group(): String = GROUP

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return Base32EncoderDecoder(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class Base64EncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    presentation = DeveloperToolPresentation("Base64", "Base64 Encoder/Decoder"),
    context = encoderDecoderContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = Base64.toBase64String(text.encodeToByteArray())

  override fun toSource(text: String): String = Base64.decode(text).decodeToString()

  override fun group(): String = GROUP

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return Base64EncoderDecoder(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class UrlBase64EncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    presentation = DeveloperToolPresentation("URL Base64", "URL Base64 Encoder/Decoder"),
    context = encoderDecoderContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = UrlBase64.encode(text.encodeToByteArray()).decodeToString()

  override fun toSource(text: String): String = UrlBase64.decode(text).decodeToString()

  override fun group(): String = GROUP

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return UrlBase64EncoderDecoder(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class UrlEncodingEncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    presentation = DeveloperToolPresentation("URL Encoding", "URL Encoding Encoder/Decoder"),
    context = encoderDecoderContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = URLEncoder.encode(text, StandardCharsets.UTF_8)

  override fun toSource(text: String): String = URLDecoder.decode(text, StandardCharsets.UTF_8)

  override fun group(): String = GROUP

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return UrlEncodingEncoderDecoder(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //
