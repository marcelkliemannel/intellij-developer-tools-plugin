package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.textescape

import org.apache.commons.text.StringEscapeUtils

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //
// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

class HtmlEntitiesTextEscape : TextEscape("html-entities", "HTML Entities") {

  override fun toTarget(text: String): String = StringEscapeUtils.escapeHtml4(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeHtml4(text)
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class JavaTextEscape : TextEscape("java-string", "Java String") {

  override fun toTarget(text: String): String = StringEscapeUtils.escapeJava(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeJava(text)
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class JsonTextEscape : TextEscape("json-string", "JSON String") {
  override fun toTarget(text: String): String = StringEscapeUtils.escapeJson(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeJson(text)
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class CsvTextEscape : TextEscape("csv-string", "CSV String") {

  override fun toTarget(text: String): String = StringEscapeUtils.escapeCsv(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeCsv(text)
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

class XmlTextEscape : TextEscape("xml-string", "XML String") {

  override fun toTarget(text: String): String = StringEscapeUtils.escapeXml11(text)

  override fun toSource(text: String): String = StringEscapeUtils.unescapeXml(text)
}