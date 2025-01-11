package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

// -- Properties -------------------------------------------------------------------------------------------------- //

private const val OBJECT_MAPPER_INDENT = "  "
private val indenter = DefaultIndenter(OBJECT_MAPPER_INDENT, System.lineSeparator())
private val prettyPrinter = DefaultPrettyPrinter()
  .withObjectIndenter(indenter)
  .withArrayIndenter(indenter)
val objectMapper: ObjectMapper = ObjectMapper()
  .setDefaultPrettyPrinter(prettyPrinter)

fun JsonNode.toPrettyStringWithDefaultObjectMapper(): String {
  if (this.isMissingNode) {
    return ""
  }

  return objectMapper
    .writerWithDefaultPrettyPrinter()
    .writeValueAsString(this)
}

// -- Exposed Methods --------------------------------------------------------------------------------------------- //
// -- Private Methods --------------------------------------------------------------------------------------------- //
// -- Inner Type -------------------------------------------------------------------------------------------------- //