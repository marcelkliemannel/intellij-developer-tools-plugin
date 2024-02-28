package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import io.ktor.util.*
import java.security.MessageDigest
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.cast

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //

inline fun <reified T> Any.safeCastTo(): T? = if (this is T) this else null

fun <T : Any> Any.uncheckedCastTo(type: KClass<T>): T = type.cast(this)

fun ByteArray.toHexMacAddress() = StringBuilder(18).also {
  for (byte in this) {
    if (isNotEmpty()) {
      it.append(':')
    }
    it.append(String.format("%02x", byte))
  }
}.toString()

fun ByteArray.toHexString(): String = HexFormat.of().formatHex(this)

fun String.toMessageDigest(): MessageDigest = MessageDigest.getInstance(this)

fun Comparator<String>.makeCaseInsensitive(): Comparator<String> {
  return Comparator { a, b ->
    this.compare(a.toLowerCasePreservingASCIIRules(), b.toLowerCasePreservingASCIIRules())
  }
}

fun Long?.compareTo(other: Long?) : Int {
  return if (this == null && other == null) {
    0
  }
  else if (this == null) {
    1
  }
  else if (other == null) {
    -1
  }
  else {
    this.compareTo(other)
  }
}

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //
