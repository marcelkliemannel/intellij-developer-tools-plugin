package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
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
  parentDisposable: Disposable,
  context: DeveloperToolContext,
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

  class Factory : DeveloperToolFactory<HtmlEntitiesEscape> {

    override fun getDeveloperToolPresentation() = DeveloperToolPresentation(
      menuTitle = "HTML Entities",
      contentTitle = "HTML Entities Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperToolContext
    ): ((DeveloperToolConfiguration) -> HtmlEntitiesEscape) =
      { configuration -> HtmlEntitiesEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JavaTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext("Java Text Escape/Unescape"),
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

  class Factory : DeveloperToolFactory<JavaTextEscape> {

    override fun getDeveloperToolPresentation() = DeveloperToolPresentation(
      menuTitle = "Java Text",
      contentTitle = "Java Text Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperToolContext
    ): ((DeveloperToolConfiguration) -> JavaTextEscape) =
      { configuration -> JavaTextEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JsonTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperToolContext,
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

  class Factory : DeveloperToolFactory<JsonTextEscape> {

    override fun getDeveloperToolPresentation() = DeveloperToolPresentation(
      menuTitle = "JSON Text",
      contentTitle = "JSON Text Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperToolContext
    ): ((DeveloperToolConfiguration) -> JsonTextEscape) =
      { configuration -> JsonTextEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class CsvTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperToolContext,
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

  class Factory : DeveloperToolFactory<CsvTextEscape> {

    override fun getDeveloperToolPresentation() = DeveloperToolPresentation(
      menuTitle = "CSV Text",
      contentTitle = "CSV Text Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperToolContext
    ): ((DeveloperToolConfiguration) -> CsvTextEscape) =
      { configuration -> CsvTextEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class XmlTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperToolContext,
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

  class Factory : DeveloperToolFactory<XmlTextEscape> {

    override fun getDeveloperToolPresentation() = DeveloperToolPresentation(
      menuTitle = "XML Text",
      contentTitle = "XML Text Escape/Unescape"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperToolContext
    ): ((DeveloperToolConfiguration) -> XmlTextEscape) =
      { configuration -> XmlTextEscape(configuration, parentDisposable, context, project) }
  }
}