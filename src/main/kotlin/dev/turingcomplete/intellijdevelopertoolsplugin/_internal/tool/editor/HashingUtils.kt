package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor

import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.toMessageDigest
import java.security.MessageDigest
import java.security.Security

object HashingUtils {
  // -- Variables --------------------------------------------------------------------------------------------------- //

  val commonHashingAlgorithms: List<MessageDigest>

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val commonHashingAlgorithms = mutableListOf<MessageDigest>()

    val availableAlgorithms = Security.getAlgorithms("MessageDigest")
    if (availableAlgorithms.contains("MD5")) {
      commonHashingAlgorithms.add("MD5".toMessageDigest())
    }
    if (availableAlgorithms.contains("SHA-1")) {
      commonHashingAlgorithms.add("SHA-1".toMessageDigest())
    }
    if (availableAlgorithms.contains("SHA-256")) {
      commonHashingAlgorithms.add("SHA-256".toMessageDigest())
    }
    if (availableAlgorithms.contains("SHA-512")) {
      commonHashingAlgorithms.add("SHA-512".toMessageDigest())
    }
    if (availableAlgorithms.contains("SHA3-256")) {
      commonHashingAlgorithms.add("SHA3-256".toMessageDigest())
    }
    if (availableAlgorithms.contains("SHA3-512")) {
      commonHashingAlgorithms.add("SHA3-512".toMessageDigest())
    }

    this.commonHashingAlgorithms = commonHashingAlgorithms
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}