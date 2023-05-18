package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.TextConverter.TextConverterContext
import org.apache.commons.text.StringEscapeUtils

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //

internal fun createEscapeUnescapeContext(title: String) = TextConverterContext(
  convertActionTitle = "Escape",
  revertActionTitle = "Unescape",
  sourceTitle = "Unescaped",
  targetTitle = "Escaped",
  diffSupport = TextConverter.DiffSupport(
    title = title
  )
)

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class HtmlEntitiesEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("HTML Entities Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeHtml4(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeHtml4(text))
  }

  class Factory : DeveloperToolFactory<HtmlEntitiesEscape> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "HTML Entities",
      contentTitle = "HTML Entities Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> HtmlEntitiesEscape) =
      { configuration -> HtmlEntitiesEscape(configuration, parentDisposable) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JavaTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("Java Text Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeJava(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeJava(text))
  }

  class Factory : DeveloperToolFactory<JavaTextEscape> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "Java Text",
      contentTitle = "Java Text Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> JavaTextEscape) =
      { configuration -> JavaTextEscape(configuration, parentDisposable) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JsonTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("JSON Text Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeJson(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeJson(text))
  }

  class Factory : DeveloperToolFactory<JsonTextEscape> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "JSON Text",
      contentTitle = "JSON Text Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> JsonTextEscape) =
      { configuration -> JsonTextEscape(configuration, parentDisposable) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class CsvTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("CSV Text Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeCsv(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeCsv(text))
  }

  class Factory : DeveloperToolFactory<CsvTextEscape> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "CSV Text",
      contentTitle = "CSV Text Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> CsvTextEscape) =
      { configuration -> CsvTextEscape(configuration, parentDisposable) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class XmlTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("XML Text Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeXml11(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeXml(text))
  }

  class Factory : DeveloperToolFactory<XmlTextEscape> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "XML Text",
      contentTitle = "XML Text Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> XmlTextEscape) =
      { configuration -> XmlTextEscape(configuration, parentDisposable) }
  }
}