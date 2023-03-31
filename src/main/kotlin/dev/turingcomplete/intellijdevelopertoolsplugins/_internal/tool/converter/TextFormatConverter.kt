package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ErrorHolder

internal class TextFormatConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextConverter(
  developerToolContext = DeveloperToolContext(
    menuTitle = "Text Format Converter",
    contentTitle = "Text Format Converter"
  ),
  textConverterContext = TextConverterContext(
    convertActionTitle = "Convert",
    revertActionTitle = "Convert",
    sourceTitle = "First",
    targetTitle = "Second",
    sourceErrorHolder = ErrorHolder(),
    targetErrorHolder = ErrorHolder()
  ),
  configuration = configuration,
  parentDisposable = parentDisposable
), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var firstLanguage = configuration.register("firstLanguage", Language.JSON)
  private var secondLanguage = configuration.register("secondLanguage", Language.YAML)

  private val codeStyles by lazy { LanguageCodeStyleSettingsProvider.EP_NAME.extensionList.associate { it.language.id to it.language } }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun configurationChanged() {
    setLanguages()
    super.configurationChanged()
  }

  override fun Panel.buildTopConfigurationUi() {
    row {
      comboBox(Language.values().toList())
        .label("First language:")
        .bindItem(firstLanguage)
    }
  }

  override fun Panel.buildMiddleSecondConfigurationUi() {
    row {
      comboBox(Language.values().toList())
        .label("Second language:")
        .bindItem(secondLanguage)
    }
  }

  override fun toTarget(text: String) {
    covert(textConverterContext.sourceErrorHolder!!) {
      targetText = secondLanguage.get().asString(firstLanguage.get().parse(text))
    }
  }

  override fun toSource(text: String) {
    covert(textConverterContext.targetErrorHolder!!) {
      sourceText = firstLanguage.get().asString(secondLanguage.get().parse(text))
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun covert(errorHolder: ErrorHolder, doConvert: () -> Unit) {
    errorHolder.unset()
    try {
      doConvert()
    } catch (e: Exception) {
      errorHolder.set(e)
    }

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `errorHolder`.
    validate()
  }

  private fun setLanguages() {
    codeStyles[firstLanguage.get().languageId]?.let { setSourceLanguage(it) }
    codeStyles[secondLanguage.get().languageId]?.let { setTargetLanguage(it) }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class Language(val title: String, val languageId: String, val objectMapper: ObjectMapper) {

    JSON("JSON", "JSON", ObjectMapper()),
    YAML("YAML", "YAML", YAMLMapper()),
    XML("XML", "XML", XmlMapper()),
    TOML("TOML", "TOML", TomlMapper()),
    PROPERTIES("Properties", "Properties", JavaPropsMapper());

    override fun toString(): String = title

    fun parse(text: String): JsonNode = objectMapper.readTree(text)

    fun asString(root: JsonNode): String = objectMapper.writeValueAsString(root)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<TextFormatConverter> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = TextFormatConverter(configuration, parentDisposable)
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}