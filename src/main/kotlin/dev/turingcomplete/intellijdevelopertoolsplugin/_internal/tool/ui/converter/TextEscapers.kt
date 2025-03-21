package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TextConverter.TextConverterContext
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolPresentation
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
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("HTML Entities Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeHtml4(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeHtml4(text))
  }

  class Factory : DeveloperUiToolFactory<HtmlEntitiesEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "HTML Entities Escaping",
      groupedMenuTitle = "HTML Entities",
      contentTitle = "HTML Entities Escape/Unescape",
      description = DeveloperUiToolPresentation.contextHelp("This tool will use <code>StringEscapeUtils.escapeHtml4(text)</code> and <code>StringEscapeUtils.unescapeHtml4(text)</code> from the 'Apache Commons Text' library.")
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> HtmlEntitiesEscape) =
      { configuration -> HtmlEntitiesEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JavaStringEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("Java String Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeJava(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeJava(text))
  }

  class Factory : DeveloperUiToolFactory<JavaStringEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Java String Escaping",
      groupedMenuTitle = "Java String",
      contentTitle = "Java String Escape/Unescape",
      description = DeveloperUiToolPresentation.contextHelp("This tool will use <code>StringEscapeUtils.escapeJava(text)</code> and <code>StringEscapeUtils.unescapeJava(text)</code> from the 'Apache Commons Text' library.")
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> JavaStringEscape) =
      { configuration -> JavaStringEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JsonTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("JSON Text Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeJson(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeJson(text))
  }

  class Factory : DeveloperUiToolFactory<JsonTextEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "JSON Text Escaping",
      groupedMenuTitle = "JSON Text",
      contentTitle = "JSON Text Escape/Unescape",
      description = DeveloperUiToolPresentation.contextHelp("This tool will use <code>StringEscapeUtils.escapeJson(text)</code> and <code>StringEscapeUtils.unescapeJson(text)</code> from the 'Apache Commons Text' library.")
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> JsonTextEscape) =
      { configuration -> JsonTextEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class CsvTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("CSV Text Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeCsv(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeCsv(text))
  }

  class Factory : DeveloperUiToolFactory<CsvTextEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "CSV Text Escaping",
      groupedMenuTitle = "CSV Text",
      contentTitle = "CSV Text Escape/Unescape",
      description = DeveloperUiToolPresentation.contextHelp("This tool will use <code>StringEscapeUtils.escapeCsv(text)</code> and <code>StringEscapeUtils.unescapeCsv(text)</code> from the 'Apache Commons Text' library.")
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> CsvTextEscape) =
      { configuration -> CsvTextEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class XmlTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("XML Text Escape/Unescape"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeXml11(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeXml(text))
  }

  class Factory : DeveloperUiToolFactory<XmlTextEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "XML Text Escaping",
      contentTitle = "XML Text Escape/Unescape",
      description = DeveloperUiToolPresentation.contextHelp("This tool will use <code>StringEscapeUtils.escapeXml11(text)</code> and <code>StringEscapeUtils.unescapeXml(text)</code> from the 'Apache Commons Text' library.")
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> XmlTextEscape) =
      { configuration -> XmlTextEscape(configuration, parentDisposable, context, project) }
  }
}