package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor

import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.toMessageDigest
import java.security.MessageDigest
import java.security.Security

object HashingUtils {
  // -- Variables --------------------------------------------------------------------------------------------------- //

  val commonMessageDigests: List<MessageDigest>

  private val algorithms = listOf<String>("MD5, SHA-1", "SHA-256", "SHA-512", "SHA3-256", "SHA3-512")

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val availableAlgorithms: Set<String> = Security.getAlgorithms("MessageDigest")
    commonMessageDigests = algorithms.mapNotNull {
      if (availableAlgorithms.contains(it)) it.toMessageDigest() else null
    }
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}