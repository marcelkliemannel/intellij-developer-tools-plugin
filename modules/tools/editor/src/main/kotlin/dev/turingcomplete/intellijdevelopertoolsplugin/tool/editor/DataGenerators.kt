package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.fasterxml.uuid.Generators
import com.github.f4b6a3.ulid.UlidCreator
import dev.turingcomplete.intellijdevelopertoolsplugin.common.HashingUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.common.toHexString
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jetbrains.annotations.Nls

object DataGenerators {
  // -- Variables ----------------------------------------------------------- //

  private val uuidV7Generator = UuidV7Generator()
  val dataGenerators: List<DataGeneratorBase> =
    listOf(
      UuidV4Generator(),
      uuidV7Generator,
      UlidGenerator(),
      NanoIdGenerator(),
      DataGeneratorsGroup(
        EditorToolsBundle.message("data-generator.group.current-date-and-time"),
        createCurrentDateAndTimeGenerators(),
      ),
      DataGeneratorsGroup(
        EditorToolsBundle.message("data-generator.group.current-unix-timestamp"),
        createUnixTimestampGenerators(),
      ),
      DataGeneratorsGroup(
        EditorToolsBundle.message("data-generator.group.random-hash"),
        createRandomHashGenerators(),
      ),
    )

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //

  private fun createRandomHashGenerators(): List<DataGenerator> =
    HashingUtils.commonMessageDigests.map { messageDigest ->
      object :
        DataGenerator(
          messageDigest.algorithm,
          EditorToolsBundle.message(
            "data-generator.action.generate-random",
            messageDigest.algorithm,
          ),
        ) {

        override fun generate(): String =
          messageDigest.digest(uuidV7Generator.generate().encodeToByteArray()).toHexString()
      }
    }

  private fun createUnixTimestampGenerators(): List<DataGenerator> =
    linkedMapOf(
        EditorToolsBundle.message("data-generator.title.seconds") to
          {
            System.currentTimeMillis().div(1000).toString()
          },
        EditorToolsBundle.message("data-generator.title.milliseconds") to
          {
            System.currentTimeMillis().toString()
          },
        EditorToolsBundle.message("data-generator.title.nanoseconds") to
          {
            System.nanoTime().toString()
          },
      )
      .map { (name, generateUnixTimestamp) ->
        object :
          DataGenerator(
            name,
            EditorToolsBundle.message(
              "data-generator.action.insert-current-unix-timestamp",
              name.lowercase(),
            ),
          ) {

          override fun generate(): String = generateUnixTimestamp()
        }
      }

  private fun createCurrentDateAndTimeGenerators(): List<DataGenerator> =
    listOf(
        Triple(
          EditorToolsBundle.message("data-generator.title.iso-8601-date-time-with-time-zone"),
          "yyyy-MM-dd'T'HH:mm:ss.SSSxxx",
          ZoneId.systemDefault(),
        ),
        Triple(
          EditorToolsBundle.message("data-generator.title.iso-8601-date-time-at-utc"),
          "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
          ZoneOffset.UTC,
        ),
        Triple(
          EditorToolsBundle.message("data-generator.title.iso-8601-date"),
          "yyyy-MM-dd",
          ZoneId.systemDefault(),
        ),
        Triple(
          EditorToolsBundle.message("data-generator.title.iso-8601-time"),
          "HH:mm:ss",
          ZoneId.systemDefault(),
        ),
        Triple(
          EditorToolsBundle.message("data-generator.title.iso-8601-ordinal-date"),
          "yyyy-DDD",
          ZoneId.systemDefault(),
        ),
        Triple(
          EditorToolsBundle.message("data-generator.title.iso-8601-week-date"),
          "YYYY-'W'ww-e",
          ZoneId.systemDefault(),
        ),
        Triple(
          EditorToolsBundle.message("data-generator.title.rfc-1123-date-time"),
          "EEE, dd MMM yyyy HH:mm:ss",
          ZoneOffset.UTC,
        ),
      )
      .map { (name, pattern, timeZone) ->
        object :
          DataGenerator(
            pattern,
            name,
            EditorToolsBundle.message("data-generator.action.insert-current-date-time", pattern),
          ) {

          override fun generate(): String =
            DateTimeFormatter.ofPattern(pattern)
              .withLocale(Locale.getDefault())
              .withZone(timeZone)
              .format(Instant.now())
        }
      }

  // -- Inner Type ---------------------------------------------------------- //

  class UuidV4Generator : DataGenerator(EditorToolsBundle.message("data-generator.title.uuid-v4")) {

    override fun generate(): String = Generators.randomBasedGenerator().generate().toString()
  }

  // -- Inner Type ---------------------------------------------------------- //

  class UuidV7Generator : DataGenerator(EditorToolsBundle.message("data-generator.title.uuid-v7")) {

    override fun generate(): String = Generators.timeBasedEpochGenerator().generate().toString()
  }

  // -- Inner Type ---------------------------------------------------------- //

  class UlidGenerator : DataGenerator(EditorToolsBundle.message("data-generator.title.ulid")) {

    override fun generate(): String = UlidCreator.getUlid().toString()
  }

  // -- Inner Type ---------------------------------------------------------- //

  class NanoIdGenerator : DataGenerator(EditorToolsBundle.message("data-generator.title.nano-id")) {

    override fun generate(): String = NanoIdUtils.randomNanoId()
  }

  // -- Inner Type ---------------------------------------------------------- //

  sealed interface DataGeneratorBase {

    val title: String
    val toolText: String?
  }

  // -- Inner Type ---------------------------------------------------------- //

  abstract class DataGenerator(
    @param:Nls(capitalization = Nls.Capitalization.Title) override val title: String,
    val actionName: String =
      EditorToolsBundle.message("data-generator.action.insert-generated", title),
    @param:Nls(capitalization = Nls.Capitalization.Sentence) override val toolText: String? = null,
  ) : DataGeneratorBase {

    abstract fun generate(): String
  }

  // -- Inner Type ---------------------------------------------------------- //

  class DataGeneratorsGroup(
    @param:Nls(capitalization = Nls.Capitalization.Title) override val title: String,
    val children: List<DataGeneratorBase>,
    @param:Nls(capitalization = Nls.Capitalization.Sentence) override val toolText: String? = null,
  ) : DataGeneratorBase
}
