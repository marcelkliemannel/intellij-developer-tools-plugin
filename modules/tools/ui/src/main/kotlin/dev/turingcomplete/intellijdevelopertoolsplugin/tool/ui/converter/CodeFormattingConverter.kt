package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.ObjectMapperService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder

class CodeFormattingConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) : TextConverter(
  textConverterContext = TextConverterContext(
    convertActionTitle = "Convert",
    revertActionTitle = "Convert",
    sourceTitle = "First",
    targetTitle = "Second",
    sourceErrorHolder = ErrorHolder(),
    targetErrorHolder = ErrorHolder(),
    diffSupport = DiffSupport(
      title = "Text Format Converter"
    )
  ),
  configuration = configuration,
  parentDisposable = parentDisposable,
  context = context,
  project = project
), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var firstLanguage = configuration.register("firstLanguage", Language.JSON)
  private var secondLanguage = configuration.register("secondLanguage", Language.YAML)

  private val codeStyles by lazy { LanguageCodeStyleSettingsProvider.EP_NAME.extensionList.associate { it.language.id to it.language } }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun configurationChanged(property: ValueProperty<out Any>) {
    setLanguages()
    super.configurationChanged(property)
  }

  override fun Panel.buildTopConfigurationUi() {
    row {
      comboBox(Language.entries)
        .label("First language:")
        .bindItem(firstLanguage)
    }
  }

  override fun Panel.buildMiddleSecondConfigurationUi() {
    row {
      comboBox(Language.entries)
        .label("Second language:")
        .bindItem(secondLanguage)
    }
  }

  override fun toTarget(text: String) {
    covert(textConverterContext.sourceErrorHolder!!) {
      if (text.isBlank()) {
        targetText.set("")
      }
      else {
        targetText.set(secondLanguage.get().asString(firstLanguage.get().parse(text)))
      }
    }
  }

  override fun toSource(text: String) {
    covert(textConverterContext.targetErrorHolder!!) {
      if (text.isBlank()) {
        sourceText.set("")
      }
      else {
        sourceText.set(firstLanguage.get().asString(secondLanguage.get().parse(text)))
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun covert(inputErrorHolder: ErrorHolder, doConvert: () -> Unit) {
    // We have to clear both `ErrorHolder`s here. If he user makes an invalid
    // input in A, which shows an error, and then edits B, the contents of A
    // would be replaced, but the error message is still visible.
    textConverterContext.targetErrorHolder!!.clear()
    textConverterContext.sourceErrorHolder!!.clear()

    try {
      doConvert()
    } catch (e: Exception) {
      inputErrorHolder.add(e)
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

  private enum class Language(
    val title: String,
    val languageId: String,
    val objectMapper: (ObjectMapperService) -> ObjectMapper
  ) {

    JSON("JSON", "JSON", { it.jsonMapper() }),
    YAML("YAML", "YAML", { it.yamlMapper() }),
    XML("XML", "XML", { it.xmlMapper() }),
    TOML("TOML", "TOML", { it.tomlMapper() }),
    PROPERTIES("Properties", "Properties", { it.javaPropsMapper() });

    override fun toString(): String = title

    fun parse(text: String): JsonNode =
      objectMapper(ObjectMapperService.instance).readTree(text)

    fun asString(root: JsonNode): String =
      objectMapper(ObjectMapperService.instance).writeValueAsString(root)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<CodeFormattingConverter> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Text Format",
      contentTitle = "Text Format Converter"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> CodeFormattingConverter) = { configuration ->
      CodeFormattingConverter(configuration, parentDisposable, context, project)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
