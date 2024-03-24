package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.toHexString
import org.apache.commons.codec.binary.Base32
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

internal object EncodersDecoders {
  // -- Variables --------------------------------------------------------------------------------------------------- //

  private val log = logger<EncodersDecoders>()

  val commonEncoders: List<Encoder>

  val commonDecoders = listOf(
    Decoder("Base32", { Base32().decode(it).decodeToString() }),
    Decoder("Base64", { Base64.getDecoder().decode(it).decodeToString() }),
    Decoder("MIME Base64", { Base64.getMimeDecoder().decode(it).decodeToString() }),
    Decoder("URL Base64", { Base64.getUrlDecoder().decode(it).decodeToString() }),
    Decoder("URL Encoding", { URLDecoder.decode(it, StandardCharsets.UTF_8) })
  )

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val commonEncoders = mutableListOf(
      Encoder("Base32", { Base32().encodeToString(it.encodeToByteArray()) }),
      Encoder("Base64", { Base64.getEncoder().encodeToString(it.encodeToByteArray()) }),
      Encoder("MIME Base64", { Base64.getMimeEncoder().encodeToString(it.encodeToByteArray()) }),
      Encoder("URL Base64", { Base64.getUrlEncoder().encodeToString(it.encodeToByteArray()) }),
      Encoder("URL Encoding", { URLEncoder.encode(it, StandardCharsets.UTF_8) }),
    )

    HashingUtils.commonHashingAlgorithms.forEach { messageDigest ->
      commonEncoders.add(Encoder(messageDigest.algorithm, { messageDigest.digest(it.encodeToByteArray()).toHexString() }))
    }

    this.commonEncoders = commonEncoders
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