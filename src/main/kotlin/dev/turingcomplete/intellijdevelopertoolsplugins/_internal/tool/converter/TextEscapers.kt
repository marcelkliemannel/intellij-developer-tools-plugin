package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import org.apache.commons.text.StringEscapeUtils

// -- Properties ---------------------------------------------------------------------------------------------------- //

private val escapeUnescapeContext = TextConverter.Context(
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
    presentation = DeveloperToolPresentation("HTML Entities", "HTML Entities Escape/Unescape"),
    context = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = StringEscapeUtils.escapeHtml4(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeHtml4(text)

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return HtmlEntitiesEscape(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JavaTextEscape(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    presentation = DeveloperToolPresentation("Java Text", "Java Text Escape/Unescape"),
    context = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = StringEscapeUtils.escapeJava(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeJava(text)

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return JavaTextEscape(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JsonTextEscape(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    presentation = DeveloperToolPresentation("JSON Text", "JSON Text Escape/Unescape"),
    context = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = StringEscapeUtils.escapeJson(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeJson(text)

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return JsonTextEscape(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class CsvTextEscape(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    presentation = DeveloperToolPresentation("CSV Text", "CSV Text Escape/Unescape"),
    context = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = StringEscapeUtils.escapeCsv(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeCsv(text)

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return CsvTextEscape(configuration, parentDisposable)
    }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class XmlTextEscape(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextConverter(
    presentation = DeveloperToolPresentation("XML Text", "XML Text Escape/Unescape"),
    context = escapeUnescapeContext,
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {

  override fun toTarget(text: String): String = StringEscapeUtils.escapeXml11(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeXml(text)

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return XmlTextEscape(configuration, parentDisposable)
    }
  }
}