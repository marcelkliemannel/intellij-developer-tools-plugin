package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.fasterxml.jackson.databind.node.ArrayNode
import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.jsonpath.JsonPathLanguage
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.JsonPathException
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ErrorHolder

class JsonPathTransformer(configuration: DeveloperToolConfiguration, parentDisposable: Disposable)
  : TextTransformer(
        presentation = DeveloperToolPresentation("JSON Path", "JSON Path Transformer"),
        transformActionTitle = "Execute Query",
        sourceTitle = "Original",
        resultTitle = "Result",
        configuration = configuration,
        parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val queryEditor: DeveloperToolEditor by lazy { createQueryInputEditor() }
  private var errorHolder = ErrorHolder()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildMiddleConfigurationUi() {
    row {
      cell(queryEditor.createComponent()).align(Align.FILL)
    }.resizableRow().topGap(TopGap.SMALL)

    row {
      icon(AllIcons.General.BalloonError).gap(RightGap.SMALL)
      label("").bindText(errorHolder.asObservableNonNullProperty())
    }.visibleIf(errorHolder.asComponentPredicate())
  }

  override fun doTransform() {
    errorHolder.error = null

    if (queryEditor.text.isBlank()) {
      return
    }

    try {
      val result = JsonPath.parse(sourceText, jsonPathConfiguration).read<Any>(queryEditor.text)
      resultText = when(result) {
        is ArrayNode -> result.toPrettyString()
        else -> result.toString()
      }
    }
    catch (e: JsonPathException) {
      errorHolder.error = "<html>${e.message}</html>"
    }
  }

  override fun getInitialOriginalText(): String = ORIGINAL_EXAMPLE

  override fun getInitialLanguage(): Language = JsonLanguage.INSTANCE

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createQueryInputEditor(): DeveloperToolEditor =
    DeveloperToolEditor(
            title = "JSON path query",
            editorMode = INPUT,
            parentDisposable = parentDisposable,
            initialLanguage = JsonPathLanguage.INSTANCE
    ).apply {
      text = ORIGINAL_JSON_PATH_EXAMPLE
      onTextChange {
        if (liveTransformation) {
          transform()
        }
      }
    }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return JsonPathTransformer(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val jsonPathConfiguration by lazy {
      Configuration.builder()
              .mappingProvider(JacksonMappingProvider())
              .jsonProvider(JacksonJsonNodeJsonProvider())
              .build()
    }

    @org.intellij.lang.annotations.Language("JSON")
    private const val ORIGINAL_EXAMPLE = """{
  "starWars": {
    "characters": [
      {
        "forename": "Luke",
        "surname": "Skywalker",
        "birthdate": "19 BBY",
        "homeWorld": "Tatooine"
      },
      {
        "forename": "Leia",
        "surname": "Organa",
        "birthdate": "19 BBY",
        "homeWorld": "Alderaan"
      },
      {
        "forename": "Ben",
        "surname": "Solo",
        "birthdate": "5 ABY",
        "homeWorld": "Chandrila"
      }
    ],
    "ships": [
      {
        "type": "Imperial Star Destroyer",
        "class": "Carrier"
      },
      {
        "type": "X-Wing Starfighter",
        "class": "Space superiority fighter"
      }
    ]
  }
}"""

    private const val ORIGINAL_JSON_PATH_EXAMPLE = "\$.starWars.characters..forename"
  }
}