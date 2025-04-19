package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.testfixtures

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.ui.JBColor
import dev.turingcomplete.intellijdevelopertoolsplugin.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin.common.random
import dev.turingcomplete.intellijdevelopertoolsplugin.common.safeCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactoryEp
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.JwtEncoderDecoder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.HmacTransformer
import java.math.BigDecimal
import java.security.Security
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random

open class DeveloperUiToolUnderTest<T : DeveloperUiTool>(
  val factoryEp: DeveloperUiToolFactoryEp<out DeveloperUiToolFactory<*>>,
  val configuration: DeveloperToolConfiguration,
  val instance: T,
) {
  // -- Properties ---------------------------------------------------------- //

  val id: String = factoryEp.id

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun randomiseConfiguration() {
    runInEdtAndWait {
      configuration.properties.forEach { key, property ->
        val randomValue = randomisePropertyValue(property)
        property.reference.setWithUncheckedCast(randomValue, null)
      }
    }
  }

  fun resetConfiguration(loadExamples: Boolean) {
    runInEdtAndWait { configuration.reset(null, loadExamples) }
  }

  fun modifyAllConfigurationProperties(
    modify: (DeveloperToolConfiguration.PropertyContainer) -> Any?
  ) {
    ApplicationManager.getApplication()
      .invokeAndWait(
        {
          configuration.properties.values.forEach {
            val modifiedValue = modify(it)
            if (modifiedValue != null) {
              it.reference.setWithUncheckedCast(modifiedValue, null)
            }
          }
        },
        ModalityState.any(),
      )
  }

  protected open fun randomisePropertyValue(
    property: DeveloperToolConfiguration.PropertyContainer
  ): Any =
    when {
      property.key == "liveConversion" -> false

      id == "code-style-formatting" && property.key == "languageId" -> "XML"
      id == "code-style-formatting" && property.key == "sourceText" -> "<foo><bar></bar></foo>"
      id == "code-style-formatting" && property.key == "targetText" -> "<foo><bar></bar></foo>"

      id == "ascii-art" && property.key == "selectedFontFileName" -> "slant.flf"

      id == "hashing-transformer" && property.key == "algorithm" ->
        Security.getAlgorithms("MessageDigest").random { it == property.defaultValue }

      id == "hmac-transformer" && property.key == "algorithm" ->
        HmacTransformer.hmacAlgorithms.random { it.algorithm == property.defaultValue }.algorithm

      id == "units-converter" && property.key == "baseConverter_baseTwoInput" ->
        randomBaseTwoString()

      id == "date-time-converter" && property.key == "timeZoneId" ->
        ZoneId.getAvailableZoneIds().random { it == property.defaultValue }

      id == "jwt-encoder-decoder" && property.key == "algorithm" ->
        JwtEncoderDecoder.SignatureAlgorithm.HMAC512
      id == "jwt-encoder-decoder" && property.key == "encodedText" ->
        "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.ANCf_8p1AE4ZQs7QuqGAyyfTEgYrKSjKWkhBk5cIn1_2QVr2jEjmM-1tu7EgnyOf_fAsvdFXva8Sv05iTGzETg"
      id == "jwt-encoder-decoder" && property.key == "headerText" ->
        """
          {
            "alg": "HS512",
            "typ": "JWT"
          }
        """
          .trimIndent()
      id == "jwt-encoder-decoder" && property.key == "payloadText" ->
        """
          {
            "sub": "1234567890",
            "name": "John Doe",
            "admin": true,
            "iat": 1516239022
          }
        """
          .trimIndent()

      else -> getRandomPropertyValueBasedOnType(property)
    }

  private fun getRandomPropertyValueBasedOnType(
    property: DeveloperToolConfiguration.PropertyContainer
  ): Any {
    val defaultValue = property.defaultValue
    return when (defaultValue) {
      is String -> randomString()
      is Int -> defaultValue + 1
      is Long -> defaultValue + 1
      is BigDecimal -> defaultValue.plus(BigDecimal.ONE)
      is Boolean -> !defaultValue
      is LocaleContainer ->
        LocaleContainer(Locale.getAvailableLocales().random { it == defaultValue.locale })

      is Enum<*> -> {
        val enumConstants = defaultValue::class.java.enumConstants
        enumConstants[(defaultValue.ordinal + 1) % enumConstants.size]
      }

      is JBColor -> JBColor(Random.nextInt(0, 255), Random.nextInt(0, 255))

      else ->
        throw IllegalStateException("Missing property type mapping for: " + defaultValue::class)
    }
  }

  protected fun randomString(): String {
    val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..20).map { allowedChars.random() }.joinToString("")
  }

  protected fun getStringPropertyValue(key: String): String =
    configuration.properties[key]?.reference?.get()?.safeCastTo()
      ?: error("Unknown property key: $key")

  // -- Private Methods ----------------------------------------------------- //

  private fun randomBaseTwoString(): String =
    (1..Random.nextInt(3, 9)).map { Random.nextInt(0, 2) }.joinToString("")

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
