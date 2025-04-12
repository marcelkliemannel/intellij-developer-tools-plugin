package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import org.apache.commons.text.StringEscapeUtils

// -- Properties ---------------------------------------------------------- //
// -- Exported Methods ---------------------------------------------------- //
// -- Private Methods  ---------------------------------------------------- //
// -- Inner Type ---------------------------------------------------------- //

class HtmlEntitiesEscaperUnescaper(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EscaperUnescaper(
    title = UiToolsBundle.message("html-entities-escaper-unescaper.title"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    StringEscapeUtils.escapeHtml4(String(source)).toByteArray()

  override fun doConvertToSource(target: ByteArray): ByteArray =
    StringEscapeUtils.unescapeHtml4(String(target)).toByteArray()

  class Factory : DeveloperUiToolFactory<HtmlEntitiesEscaperUnescaper> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("html-entities-escaper-unescaper.title"),
        groupedMenuTitle =
          UiToolsBundle.message("html-entities-escaper-unescaper.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("html-entities-escaper-unescaper.content-title"),
        description =
          DeveloperUiToolPresentation.contextHelp(
            UiToolsBundle.message("html-entities-escaper-unescaper.description.context-help")
          ),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> HtmlEntitiesEscaperUnescaper) = { configuration ->
      HtmlEntitiesEscaperUnescaper(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class JavaStringEscaperUnescaper(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EscaperUnescaper(
    title = UiToolsBundle.message("java-string-escaper-unescaper.title"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    StringEscapeUtils.escapeJava(String(source)).toByteArray()

  override fun doConvertToSource(target: ByteArray): ByteArray =
    StringEscapeUtils.unescapeJava(String(target)).toByteArray()

  class Factory : DeveloperUiToolFactory<JavaStringEscaperUnescaper> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("java-string-escaper-unescaper.title"),
        groupedMenuTitle =
          UiToolsBundle.message("java-string-escaper-unescaper.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("java-string-escaper-unescaper.content-title"),
        description =
          DeveloperUiToolPresentation.contextHelp(
            UiToolsBundle.message("java-string-escaper-unescaper.description.context-help")
          ),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> JavaStringEscaperUnescaper) = { configuration ->
      JavaStringEscaperUnescaper(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class JsonTextEscaperUnescaper(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EscaperUnescaper(
    title = UiToolsBundle.message("json-text-escaper-unescaper.title"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    StringEscapeUtils.escapeJson(String(source)).toByteArray()

  override fun doConvertToSource(target: ByteArray): ByteArray =
    StringEscapeUtils.unescapeJson(String(target)).toByteArray()

  class Factory : DeveloperUiToolFactory<JsonTextEscaperUnescaper> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("json-text-escaper-unescaper.title"),
        groupedMenuTitle = UiToolsBundle.message("json-text-escaper-unescaper.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("json-text-escaper-unescaper.content-title"),
        description =
          DeveloperUiToolPresentation.contextHelp(
            UiToolsBundle.message("json-text-escaper-unescaper.description.context-help")
          ),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> JsonTextEscaperUnescaper) = { configuration ->
      JsonTextEscaperUnescaper(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class CsvTextEscaperUnescaper(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EscaperUnescaper(
    title = UiToolsBundle.message("csv-text-escaper-unescaper.title"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    StringEscapeUtils.escapeCsv(String(source)).toByteArray()

  override fun doConvertToSource(target: ByteArray): ByteArray =
    StringEscapeUtils.unescapeCsv(String(target)).toByteArray()

  class Factory : DeveloperUiToolFactory<CsvTextEscaperUnescaper> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("csv-text-escaper-unescaper.title"),
        groupedMenuTitle = UiToolsBundle.message("csv-text-escaper-unescaper.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("csv-text-escaper-unescaper.content-title"),
        description =
          DeveloperUiToolPresentation.contextHelp(
            UiToolsBundle.message("csv-text-escaper-unescaper.description.context-help")
          ),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> CsvTextEscaperUnescaper) = { configuration ->
      CsvTextEscaperUnescaper(configuration, parentDisposable, context, project)
    }
  }
}

// -- Inner Type ---------------------------------------------------------- //

class XmlTextEscaperUnescaper(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?,
) :
  EscaperUnescaper(
    title = UiToolsBundle.message("xml-text-escaper-unescaper.title"),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project,
  ) {

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    StringEscapeUtils.escapeXml11(String(source)).toByteArray()

  override fun doConvertToSource(target: ByteArray): ByteArray =
    StringEscapeUtils.unescapeXml(String(target)).toByteArray()

  class Factory : DeveloperUiToolFactory<XmlTextEscaperUnescaper> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("xml-text-escaper-unescaper.title"),
        groupedMenuTitle = UiToolsBundle.message("xml-text-escaper-unescaper.grouped-menu-title"),
        contentTitle = UiToolsBundle.message("xml-text-escaper-unescaper.content-title"),
        description =
          DeveloperUiToolPresentation.contextHelp(
            UiToolsBundle.message("xml-text-escaper-unescaper.description.context-help")
          ),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> XmlTextEscaperUnescaper) = { configuration ->
      XmlTextEscaperUnescaper(configuration, parentDisposable, context, project)
    }
  }
}
