package dev.turingcomplete.intellijdevelopertoolsplugins

import java.security.MessageDigest

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //

inline fun <reified T> Any.safeCastTo(): T? = if (this is T) this else null

fun ByteArray.toHexMacAddress() = StringBuilder(18).also {
  for (byte in this) {
    if (isNotEmpty()) {
      it.append(':')
    }
    it.append(String.format("%02x", byte))
  }
}.toString()

fun String.toMessageDigest(): MessageDigest = MessageDigest.getInstance(this)

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //
