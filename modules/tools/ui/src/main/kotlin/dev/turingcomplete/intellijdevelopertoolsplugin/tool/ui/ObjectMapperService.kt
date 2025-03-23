package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings

@Service(Service.Level.APP)
class ObjectMapperService {
  // -- Properties ---------------------------------------------------------- //

  private var jsonMapper: JsonMapper? = null
  private var lastJsonHandlingModificationsCounter: Int = 0

  private val yamlMapper: YAMLMapper = YAMLMapper()
  private val xmlMapper: XmlMapper = XmlMapper()
  private val tomlMapper: TomlMapper = TomlMapper()
  private val javaPropsMapper: JavaPropsMapper = JavaPropsMapper()

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun jsonMapper(): JsonMapper {
    if (jsonMapper == null || lastJsonHandlingModificationsCounter != DeveloperToolsApplicationSettings.jsonHandling.modificationsCounter) {
      jsonMapper = createJsonMapper()
      lastJsonHandlingModificationsCounter = DeveloperToolsApplicationSettings.jsonHandling.modificationsCounter
    }

    return jsonMapper!!
  }

  fun yamlMapper(): YAMLMapper = yamlMapper

  fun xmlMapper(): XmlMapper = xmlMapper

  fun tomlMapper(): TomlMapper = tomlMapper

  fun javaPropsMapper(): JavaPropsMapper = javaPropsMapper

  fun prettyPrintJson(jsonNode: JsonNode): String {
    if (jsonNode.isMissingNode) {
      return ""
    }

    return jsonMapper()
      .writerWithDefaultPrettyPrinter()
      .writeValueAsString(this)
  }

  // -- Private Methods ----------------------------------------------------- //

  fun createJsonMapper(): JsonMapper {
    val settings = DeveloperToolsApplicationSettings.jsonHandling

    return JsonMapper.builder().apply {
      // Writing settings
      if (settings.writeQuoteFieldNames.get()) {
        enable(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature())
      } else {
        disable(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature())
      }

      if (settings.writeNanAsStrings.get()) {
        enable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS)
      } else {
        disable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS)
      }

      if (settings.writeNumbersAsStrings.get()) {
        enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.mappedFeature())
      } else {
        disable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.mappedFeature())
      }

      if (settings.writeEscapeNonAscii.get()) {
        enable(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature())
      } else {
        disable(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature())
      }

      if (settings.writeHexUpperCase.get()) {
        enable(JsonWriteFeature.WRITE_HEX_UPPER_CASE.mappedFeature())
      } else {
        disable(JsonWriteFeature.WRITE_HEX_UPPER_CASE.mappedFeature())
      }

      val indenter = DefaultIndenter(" ".repeat(settings.writeIntentionSpaces.get()), System.lineSeparator())
      val prettyPrinter = DefaultPrettyPrinter()
        .withObjectIndenter(indenter)
        .withArrayIndenter(indenter)
      defaultPrettyPrinter(prettyPrinter)

      // Reading settings
      if (settings.readAllowJavaComments.get()) {
        enable(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature())
      }
      if (settings.readAllowYamlComments.get()) {
        enable(JsonReadFeature.ALLOW_YAML_COMMENTS.mappedFeature())
      }
      if (settings.readAllowSingleQuotes.get()) {
        enable(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature())
      }
      if (settings.readAllowUnquotedFieldNames.get()) {
        enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature())
      }
      if (settings.readAllowUnescapedControlChars.get()) {
        enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
      }
      if (settings.readAllowBackslashEscapingAnyCharacter.get()) {
        enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature())
      }
      if (settings.readAllowLeadingZerosForNumbers.get()) {
        enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature())
      }
      if (settings.readAllowLeadingPlusSignForNumbers.get()) {
        enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS.mappedFeature())
      }
      if (settings.readAllowLeadingDecimalPointForNumbers.get()) {
        enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS.mappedFeature())
      }
      if (settings.readAllowTrailingDecimalPointForNumbers.get()) {
        enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS.mappedFeature())
      }
      if (settings.readAllowNonNumericNumbers.get()) {
        enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS)
      }
      if (settings.readAllowMissingValues.get()) {
        enable(JsonReadFeature.ALLOW_MISSING_VALUES.mappedFeature())
      }
      if (settings.readAllowTrailingComma.get()) {
        enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())
      }
    }.build()
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    val instance: ObjectMapperService
      get() = service()
  }
}
