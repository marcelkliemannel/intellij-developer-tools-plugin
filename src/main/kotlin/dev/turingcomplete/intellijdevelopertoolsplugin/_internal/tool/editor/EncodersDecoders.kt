package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.EditorUtils.executeWriteCommand
import org.apache.commons.codec.binary.Base32
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

internal object EncodersDecoders {
  // -- Variables --------------------------------------------------------------------------------------------------- //

  private val log = logger<EncodersDecoders>()

  val encoderDecoders = listOf(
    EncoderDecoder(
      "Base32",
      { Base32().encodeToString(it.encodeToByteArray()) },
      { Base32().decode(it).decodeToString() }
    ),
    EncoderDecoder(
      "Base64",
      { Base64.getEncoder().encodeToString(it.encodeToByteArray()) },
      { Base64.getDecoder().decode(it).decodeToString() }
    ),
    EncoderDecoder(
      "MIME Base64",
      { Base64.getMimeEncoder().encodeToString(it.encodeToByteArray()) },
      { Base64.getMimeDecoder().decode(it).decodeToString() }
    ),
    EncoderDecoder(
      "URL Base64",
      { Base64.getUrlEncoder().encodeToString(it.encodeToByteArray()) },
      { Base64.getUrlDecoder().decode(it).decodeToString() }
    ),
    EncoderDecoder(
      "URL Encoding",
      { URLEncoder.encode(it, StandardCharsets.UTF_8) },
      { URLDecoder.decode(it, StandardCharsets.UTF_8) }
    )
  )

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun executeTransformationInEditor(
    text: String,
    textRange: TextRange,
    transformationMode: TransformationMode,
    encoderDecoder: EncoderDecoder,
    editor: Editor
  ) {
    try {
      val (result, actionName) = when (transformationMode) {
        TransformationMode.ENCODE -> encoderDecoder.encode(text) to "Encode to ${encoderDecoder.title}"
        TransformationMode.DECODE -> encoderDecoder.decode(text) to "Decode from ${encoderDecoder.title}"
      }
      editor.executeWriteCommand(actionName) {
        it.document.replaceString(textRange.startOffset, textRange.endOffset, result)
      }
    }
    catch (e: Exception) {
      log.error("Encoding/Decoding failed", e)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(editor.project, "Encoding/Decoding failed: ${e.message}", "Encode/Decode")
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class EncoderDecoder(val title: String, val encode: (String) -> String, val decode: (String) -> String)

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class TransformationMode(val actionName: String) {

    ENCODE("Encode To"),
    DECODE("Decode From")
  }
}