package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.emptyByteArray
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.ObjectMapperService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.BidirectionalConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.TextInputOutputHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

class TextFormattingConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  BidirectionalConverter(
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
    title = UiToolsBundle.message("code-formatting.title"),
    sourceTitle = UiToolsBundle.message("code-formatting.first-title"),
    targetTitle = UiToolsBundle.message("code-formatting.second-title"),
    toSourceTitle = UiToolsBundle.message("code-formatting.to-first-title"),
    toTargetTitle = UiToolsBundle.message("code-formatting.to-source-title"),
  ),
  DeveloperToolConfiguration.ChangeListener {
  // -- Properties ---------------------------------------------------------- //

  private var sourceLanguage = configuration.register("firstLanguage", Language.JSON)
  private var targetLanguage = configuration.register("secondLanguage", Language.YAML)

  private lateinit var sourceTextInputOutputHandler: TextInputOutputHandler
  private lateinit var targetTextInputOutputHandler: TextInputOutputHandler

  private val codeStyles by lazy {
    LanguageCodeStyleSettingsProvider.EP_NAME.extensionList.associate {
      it.language.id to it.language
    }
  }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    sourceTextInputOutputHandler = addTextInputOutputHandler(defaultSourceInputOutputHandlerId)
  }

  override fun ConversionSideHandler.addTargetTextInputOutputHandler() {
    targetTextInputOutputHandler = addTextInputOutputHandler(defaultTargetInputOutputHandlerId)
  }

  override fun afterBuildUi() {
    super.afterBuildUi()
    syncLanguages()
  }

  override fun configurationChanged(property: ValueProperty<out Any>) {
    super.configurationChanged(property)
    syncLanguages()
  }

  override fun Panel.buildSourceTopConfigurationUi() {
    row {
      comboBox(Language.entries)
        .label(UiToolsBundle.message("code-formatting.first-language"))
        .bindItem(sourceLanguage)
    }
  }

  override fun Panel.buildTargetTopConfigurationUi() {
    row {
      comboBox(Language.entries)
        .label(UiToolsBundle.message("code-formatting.second-language"))
        .bindItem(targetLanguage)
    }
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    if (source.isEmpty()) {
      emptyByteArray
    } else {
      targetLanguage.get().writeAsBytes(sourceLanguage.get().parse(source))
    }

  override fun doConvertToSource(target: ByteArray): ByteArray =
    if (target.isEmpty()) {
      emptyByteArray
    } else {
      sourceLanguage.get().writeAsBytes(targetLanguage.get().parse(target))
    }

  // -- Private Methods ----------------------------------------------------- //

  private fun syncLanguages() {
    val sourceCodeStyle = codeStyles[sourceLanguage.get().languageId]
    sourceTextInputOutputHandler.setLanguage(sourceCodeStyle ?: PlainTextLanguage.INSTANCE)

    val targetCodeStyle = codeStyles[targetLanguage.get().languageId]
    targetTextInputOutputHandler.setLanguage(targetCodeStyle ?: PlainTextLanguage.INSTANCE)
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class Language(
    val title: String,
    val languageId: String,
    val objectMapper: (ObjectMapperService) -> ObjectMapper,
  ) {

    JSON(UiToolsBundle.message("code-formatting.json-title"), "JSON", { it.jsonMapper() }),
    YAML(UiToolsBundle.message("code-formatting.yaml-title"), "YAML", { it.yamlMapper() }),
    XML(UiToolsBundle.message("code-formatting.xml-title"), "XML", { it.xmlMapper() }),
    TOML(UiToolsBundle.message("code-formatting.toml-title"), "TOML", { it.tomlMapper() }),
    PROPERTIES(
      UiToolsBundle.message("code-formatting.properties-title"),
      "Properties",
      { it.javaPropsMapper() },
    );

    override fun toString(): String = title

    fun parse(text: ByteArray): JsonNode = objectMapper(ObjectMapperService.instance).readTree(text)

    fun writeAsBytes(root: JsonNode): ByteArray =
      objectMapper(ObjectMapperService.instance).writeValueAsBytes(root)
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextFormattingConverter> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("code-formatting.title"),
        contentTitle = UiToolsBundle.message("code-formatting.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> TextFormattingConverter) = { configuration ->
      TextFormattingConverter(configuration, parentDisposable, context, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
