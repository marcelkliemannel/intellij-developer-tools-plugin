package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.uuid

import com.fasterxml.uuid.Generators
import dev.turingcomplete.intellijdevelopertoolsplugins.toMessageDigest

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //
// -- Private Methods ----------------------------------------------------------------------------------------------- //

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UuidV1Generator : MacAddressBasedUuidGenerator("UUIDv1") {

  override fun generate(): String = Generators.timeBasedGenerator(getEthernetAddress()).generate().toString()
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UuidV3Generator : NamespaceAndNameBasedUuidGenerator("UUIDv3", "MD5".toMessageDigest())

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UuidV4Generator : UuidGenerator("UUIDv4") {

  override fun generate(): String = Generators.randomBasedGenerator().generate().toString()
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UuidV5Generator : NamespaceAndNameBasedUuidGenerator(title = "UUIDv5", "SHA-1".toMessageDigest())

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UuidV6Generator : MacAddressBasedUuidGenerator("UUIDv6") {

  override fun generate(): String = Generators.timeBasedReorderedGenerator(getEthernetAddress()).generate().toString()
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class UuidV7Generator : UuidGenerator("UUIDv7") {

  override fun generate(): String = Generators.timeBasedEpochGenerator().generate().toString()
}