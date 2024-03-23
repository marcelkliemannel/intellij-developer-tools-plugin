package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.toHexString
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.toMessageDigest
import org.apache.commons.codec.binary.Base32
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.Security
import java.util.*

internal object EncodersDecoders {
  // -- Variables --------------------------------------------------------------------------------------------------- //

  private val log = logger<EncodersDecoders>()

  val encoders: List<Encoder>

  val decoders = listOf(
    Decoder("Base32", { Base32().decode(it).decodeToString() }),
    Decoder("Base64", { Base64.getDecoder().decode(it).decodeToString() }),
    Decoder("MIME Base64", { Base64.getMimeDecoder().decode(it).decodeToString() }),
    Decoder("URL Base64", { Base64.getUrlDecoder().decode(it).decodeToString() }),
    Decoder("URL Encoding", { URLDecoder.decode(it, StandardCharsets.UTF_8) })
  )

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val encoders = mutableListOf(
      Encoder("Base32", { Base32().encodeToString(it.encodeToByteArray()) }),
      Encoder("Base64", { Base64.getEncoder().encodeToString(it.encodeToByteArray()) }),
      Encoder("MIME Base64", { Base64.getMimeEncoder().encodeToString(it.encodeToByteArray()) }),
      Encoder("URL Base64", { Base64.getUrlEncoder().encodeToString(it.encodeToByteArray()) }),
      Encoder("URL Encoding", { URLEncoder.encode(it, StandardCharsets.UTF_8) }),
    )

    val availableAlgorithms = Security.getAlgorithms("MessageDigest")
    if (availableAlgorithms.contains("MD5")) {
      encoders.add(Encoder("MD5", { "MD5".toMessageDigest().digest(it.encodeToByteArray()).toHexString() }))
    }
    if (availableAlgorithms.contains("SHA-1")) {
      encoders.add(Encoder("SHA-1", { "SHA-1".toMessageDigest().digest(it.encodeToByteArray()).toHexString() }))
    }
    if (availableAlgorithms.contains("SHA-256")) {
      encoders.add(Encoder("SHA-256", { "SHA-256".toMessageDigest().digest(it.encodeToByteArray()).toHexString() }))
    }
    if (availableAlgorithms.contains("SHA-512")) {
      encoders.add(Encoder("SHA-512", { "SHA-512".toMessageDigest().digest(it.encodeToByteArray()).toHexString() }))
    }
    if (availableAlgorithms.contains("SHA3-256")) {
      encoders.add(Encoder("SHA3-256", { "SHA3-256".toMessageDigest().digest(it.encodeToByteArray()).toHexString() }))
    }
    if (availableAlgorithms.contains("SHA3-512")) {
      encoders.add(Encoder("SHA3-512", { "SHA3-512".toMessageDigest().digest(it.encodeToByteArray()).toHexString() }))
    }

    this.encoders = encoders
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun executeEncodingInEditor(
    text: String,
    textRange: TextRange,
    encoder: Encoder,
    editor: Editor
  ) {
    try {
      val result = encoder.encode(text)
      editor.executeWriteCommand(encoder.actionName) {
        it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
      }
    }
    catch (e: Exception) {
      log.warn("Encoding failed", e)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(editor.project, "Encoding failed: ${e.message}", encoder.actionName)
      }
    }
  }

  fun executeDecodingInEditor(
    text: String,
    textRange: TextRange,
    decoder: Decoder,
    editor: Editor
  ) {
    try {
      val result = decoder.decode(text)
      editor.executeWriteCommand(decoder.actionName) {
        it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
      }
    }
    catch (e: Exception) {
      log.warn("Decoding failed", e)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(editor.project, "Decoding failed: ${e.message}", decoder.actionName)
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Encoder(val title: String, val encode: (String) -> String, val actionName: String = "Encode to $title")

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Decoder(val title: String, val decode: (String) -> String, val actionName: String = "Decode from $title")
}