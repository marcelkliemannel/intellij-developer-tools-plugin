package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.intellijdevelopertoolsplugin.common.HashingUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.common.decodeFromAscii
import dev.turingcomplete.intellijdevelopertoolsplugin.common.encodeToAscii
import dev.turingcomplete.intellijdevelopertoolsplugin.common.toHexString
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.apache.commons.codec.binary.Base32

object EncodersDecoders {
  // -- Variables ----------------------------------------------------------- //

  private val log = logger<EncodersDecoders>()

  val commonEncoders: List<Encoder>

  val commonDecoders =
    listOf(
      Decoder(
        EditorToolsBundle.message("encode-decode.title.base32"),
        { Base32().decode(it).decodeToString() },
      ),
      Decoder(
        EditorToolsBundle.message("encode-decode.title.base64"),
        { Base64.getDecoder().decode(it).decodeToString() },
      ),
      Decoder(
        EditorToolsBundle.message("encode-decode.title.mime-base64"),
        { Base64.getMimeDecoder().decode(it).decodeToString() },
      ),
      Decoder(
        EditorToolsBundle.message("encode-decode.title.url-base64"),
        { Base64.getUrlDecoder().decode(it).decodeToString() },
      ),
      Decoder(
        EditorToolsBundle.message("encode-decode.title.url"),
        { URLDecoder.decode(it, StandardCharsets.UTF_8) },
      ),
      Decoder(EditorToolsBundle.message("encode-decode.title.ascii"), { it.decodeFromAscii() }),
    )

  // -- Initialization ------------------------------------------------------ //

  init {
    val commonEncoders =
      mutableListOf(
        Encoder(
          EditorToolsBundle.message("encode-decode.title.base32"),
          { Base32().encodeToString(it.encodeToByteArray()) },
        ),
        Encoder(
          EditorToolsBundle.message("encode-decode.title.base64"),
          { Base64.getEncoder().encodeToString(it.encodeToByteArray()) },
        ),
        Encoder(
          EditorToolsBundle.message("encode-decode.title.mime-base64"),
          { Base64.getMimeEncoder().encodeToString(it.encodeToByteArray()) },
        ),
        Encoder(
          EditorToolsBundle.message("encode-decode.title.url-base64"),
          { Base64.getUrlEncoder().encodeToString(it.encodeToByteArray()) },
        ),
        Encoder(
          EditorToolsBundle.message("encode-decode.title.url"),
          { URLEncoder.encode(it, StandardCharsets.UTF_8) },
        ),
        Encoder(EditorToolsBundle.message("encode-decode.title.ascii"), { it.encodeToAscii() }),
      )

    HashingUtils.commonMessageDigests.forEach { messageDigest ->
      commonEncoders.add(
        Encoder(
          messageDigest.algorithm,
          { messageDigest.digest(it.encodeToByteArray()).toHexString() },
        )
      )
    }

    this.commonEncoders = commonEncoders
  }

  // -- Exported Methods ---------------------------------------------------- //

  fun executeEncodingInEditor(
    text: String,
    textRange: TextRange,
    encoder: Encoder,
    editor: Editor,
  ) {
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        val result = encoder.encode(text)
        ApplicationManager.getApplication().invokeLater {
          if (!editor.isDisposed && editor.document.isWritable) {
            editor.executeWriteCommand(encoder.actionName) {
              it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
            }
          }
        }
      } catch (e: Exception) {
        log.warn("Encoding failed", e)
        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog(
            editor.project,
            EditorToolsBundle.message("encode-decode.error.encoding-failed", e.message ?: ""),
            encoder.actionName,
          )
        }
      }
    }
  }

  fun executeDecodingInEditor(
    text: String,
    textRange: TextRange,
    decoder: Decoder,
    editor: Editor,
  ) {
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        val result = decoder.decode(text)
        ApplicationManager.getApplication().invokeLater {
          if (!editor.isDisposed && editor.document.isWritable) {
            editor.executeWriteCommand(decoder.actionName) {
              it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
            }
          }
        }
      } catch (e: Exception) {
        log.warn("Decoding failed", e)
        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog(
            editor.project,
            EditorToolsBundle.message("encode-decode.error.decoding-failed", e.message ?: ""),
            decoder.actionName,
          )
        }
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Encoder(
    val title: String,
    val encode: (String) -> String,
    val actionName: String = EditorToolsBundle.message("encode-decode.action.encode-to", title),
  )

  // -- Inner Type ---------------------------------------------------------- //

  class Decoder(
    val title: String,
    val decode: (String) -> String,
    val actionName: String = EditorToolsBundle.message("encode-decode.action.decode-from", title),
  )
}
