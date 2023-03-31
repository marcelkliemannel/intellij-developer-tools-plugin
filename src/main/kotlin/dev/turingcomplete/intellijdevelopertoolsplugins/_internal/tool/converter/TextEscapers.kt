package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import org.apache.commons.text.StringEscapeUtils

// -- Properties ---------------------------------------------------------------------------------------------------- //

private val escapeUnescapeContext = TextConverter.TextConverterContext(
  convertActionTitle = "Escape",
  revertActionTitle = "Unescape",
  sourceTitle = "Unescaped",
  targetTitle = "Escaped"
)

// -- Exposed Methods ----------------------------------------------------------------------------------------------- //
// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class HtmlEntitiesEscape(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    developerToolContext = DeveloperToolContext("HTML Entities", "HTML Entities Escape/Unescape"),
    textConverterContext = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText = StringEscapeUtils.escapeHtml4(text)
  }

  override fun toSource(text: String) {
    sourceText = StringEscapeUtils.unescapeHtml4(text)
  }

  class Factory : DeveloperToolFactory<HtmlEntitiesEscape> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = HtmlEntitiesEscape(configuration, parentDisposable)
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JavaTextEscape(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    developerToolContext = DeveloperToolContext("Java Text", "Java Text Escape/Unescape"),
    textConverterContext = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText = StringEscapeUtils.escapeJava(text)
  }

  override fun toSource(text: String) {
    sourceText = StringEscapeUtils.unescapeJava(text)
  }

  class Factory : DeveloperToolFactory<JavaTextEscape> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = JavaTextEscape(configuration, parentDisposable)
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JsonTextEscape(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    developerToolContext = DeveloperToolContext("JSON Text", "JSON Text Escape/Unescape"),
    textConverterContext = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText = StringEscapeUtils.escapeJson(text)
  }

  override fun toSource(text: String) {
    sourceText = StringEscapeUtils.unescapeJson(text)
  }

  class Factory : DeveloperToolFactory<JsonTextEscape> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = JsonTextEscape(configuration, parentDisposable)
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class CsvTextEscape(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    developerToolContext = DeveloperToolContext("CSV Text", "CSV Text Escape/Unescape"),
    textConverterContext = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText = StringEscapeUtils.escapeCsv(text)
  }

  override fun toSource(text: String) {
    sourceText = StringEscapeUtils.unescapeCsv(text)
  }

  class Factory : DeveloperToolFactory<CsvTextEscape> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = CsvTextEscape(configuration, parentDisposable)
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class XmlTextEscape(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    developerToolContext = DeveloperToolContext("XML Text", "XML Text Escape/Unescape"),
    textConverterContext = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText = StringEscapeUtils.escapeXml11(text)
  }

  override fun toSource(text: String) {
    sourceText = StringEscapeUtils.unescapeXml(text)
  }

  class Factory : DeveloperToolFactory<XmlTextEscape> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = XmlTextEscape(configuration, parentDisposable)
  }
}