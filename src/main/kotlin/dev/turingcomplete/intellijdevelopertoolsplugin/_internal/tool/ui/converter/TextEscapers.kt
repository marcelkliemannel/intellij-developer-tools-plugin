package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TextConverter.TextConverterContext
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
      menuTitle = "HTML Entities",
      contentTitle = "HTML Entities Escape/Unescape"
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

internal class JavaTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
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

  class Factory : DeveloperUiToolFactory<JavaTextEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Java Text",
      contentTitle = "Java Text Escape/Unescape"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> JavaTextEscape) =
      { configuration -> JavaTextEscape(configuration, parentDisposable, context, project) }
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
      menuTitle = "JSON Text",
      contentTitle = "JSON Text Escape/Unescape"
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
      menuTitle = "CSV Text",
      contentTitle = "CSV Text Escape/Unescape"
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
      menuTitle = "XML Text",
      contentTitle = "XML Text Escape/Unescape"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> XmlTextEscape) =
      { configuration -> XmlTextEscape(configuration, parentDisposable, context, project) }
  }
}