package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.fasterxml.uuid.Generators
import com.github.f4b6a3.ulid.UlidCreator
import org.jetbrains.annotations.Nls
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

internal object DataGenerators {
  // -- Variables --------------------------------------------------------------------------------------------------- //

  private val uuidV7Generator = UuidV7Generator()
  val dataGenerators: List<DataGeneratorBase> = listOf(
    UuidV4Generator(),
    uuidV7Generator,
    UlidGenerator(),
    NanoIdGenerator(),
    DataGeneratorsGroup("Current Date and Time", createCurrentDateAndTimeGenerators()),
    DataGeneratorsGroup("Current Unix Timestamp", createUnixTimestampGenerators()),
    DataGeneratorsGroup("Random Hash", createRandomHashGenerators())
  )

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //


  private fun createRandomHashGenerators(): List<DataGenerator> =
    HashingUtils.commonHashingAlgorithms.map { messageDigest ->
      object : DataGenerator(messageDigest.algorithm, "Generate random ${messageDigest.algorithm}") {

        @OptIn(ExperimentalStdlibApi::class)
        override fun generate(): String =
          messageDigest.digest(uuidV7Generator.generate().encodeToByteArray()).toHexString()
      }
    }

  private fun createUnixTimestampGenerators(): List<DataGenerator> =
    linkedMapOf(
      "Seconds" to { System.currentTimeMillis().div(1000).toString() },
      "Milliseconds" to { System.currentTimeMillis().toString() },
      "Nanoseconds" to { System.nanoTime().toString() },
    ).map { (name, generateUnixTimestamp) ->
      object : DataGenerator(name, "Insert current UNIX timestamp (${name.lowercase()})") {

        override fun generate(): String = generateUnixTimestamp()
      }
    }

  private fun createCurrentDateAndTimeGenerators(): List<DataGenerator> =
    listOf(
      Triple("ISO-8601 date time with time zone", "yyyy-MM-dd'T'HH:mm:ss.SSSxxx", ZoneId.systemDefault()),
      Triple("ISO-8601 date time at UTC", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", ZoneOffset.UTC),
      Triple("ISO-8601 date", "yyyy-MM-dd", ZoneId.systemDefault()),
      Triple("ISO-8601 time", "HH:mm:ss", ZoneId.systemDefault()),
      Triple("ISO-8601 ordinal date", "yyyy-DDD", ZoneId.systemDefault()),
      Triple("ISO-8601 week date", "YYYY-'W'ww-e", ZoneId.systemDefault()),
      Triple("RFC-1123 date time", "EEE, dd MMM yyyy HH:mm:ss", ZoneOffset.UTC)
    ).map { (name, pattern, timeZone) ->
      object : DataGenerator(pattern, name,"Insert current date time using format: $pattern") {

        override fun generate(): String =
          DateTimeFormatter.ofPattern(pattern)
            .withLocale(Locale.getDefault())
            .withZone(timeZone)
            .format(Instant.now())
      }
    }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class UuidV4Generator : DataGenerator("UUIDv4") {

    override fun generate(): String = Generators.randomBasedGenerator().generate().toString()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class UuidV7Generator : DataGenerator("UUIDv7") {

    override fun generate(): String = Generators.timeBasedEpochGenerator().generate().toString()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class UlidGenerator : DataGenerator("ULID") {

    override fun generate(): String = UlidCreator.getUlid().toString()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class NanoIdGenerator : DataGenerator("Nano ID") {

    override fun generate(): String = NanoIdUtils.randomNanoId()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  sealed interface DataGeneratorBase {

    val title: String
    val toolText: String?
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  abstract class DataGenerator(
    @Nls(capitalization = Nls.Capitalization.Title) override val title: String,
    val actionName: String = "Insert generated $title",
    @Nls(capitalization = Nls.Capitalization.Sentence) override val toolText: String? = null
  ) : DataGeneratorBase {

    abstract fun generate(): String
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class DataGeneratorsGroup(
    @Nls(capitalization = Nls.Capitalization.Title) override val title: String,
    val children: List<DataGeneratorBase>,
    @Nls(capitalization = Nls.Capitalization.Sentence) override val toolText: String? = null
  ) : DataGeneratorBase
}