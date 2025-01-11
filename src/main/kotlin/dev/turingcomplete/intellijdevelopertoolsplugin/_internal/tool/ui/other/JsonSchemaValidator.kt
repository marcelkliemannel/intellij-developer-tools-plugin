package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersionDetector
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.PropertyComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.PropertyType
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolPresentation

class JsonSchemaValidator(
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  private val project: Project?
) : DeveloperUiTool(parentDisposable) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var liveValidation = configuration.register("liveValidation", true)
  private val schemaText = configuration.register("schemaText", "", PropertyType.INPUT, EXAMPLE_SCHEMA)
  private val dataText = configuration.register("dataText", "", PropertyType.INPUT, EXAMPLE_DATA)

  private val schemaEditor by lazy { this.createSchemaEditor() }
  private val schemaErrorHolder = ErrorHolder()
  private val dataEditor by lazy { this.createDataEditor() }
  private val dataErrorHolder = ErrorHolder()

  private val validationState: ObservableMutableProperty<ValidationState> = AtomicProperty(ValidationState.VALIDATED)
  private val validationError: ObservableMutableProperty<String> = AtomicProperty("")

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    liveValidation.afterChange {
      if (it) {
        validateSchema()
      }
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      cell(schemaEditor.component).align(Align.FILL)
        .validationOnApply(schemaEditor.bindValidator(schemaErrorHolder.asValidation()))
        .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
    }.resizableRow()

    row {
      val liveValidationCheckBox = checkBox("Live validation")
        .bindSelected(liveValidation)
        .gap(RightGap.SMALL)

      button("Validate") { validateSchema() }
        .enabledIf(liveValidationCheckBox.selected.not())
        .gap(RightGap.SMALL)
    }

    row {
      cell(dataEditor.component).align(Align.FILL)
        .validationOnApply(dataEditor.bindValidator(dataErrorHolder.asValidation()))
        .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
    }.resizableRow()

    row {
      icon(AllIcons.General.InspectionsOK).gap(RightGap.SMALL)
      label("Data matches schema")
    }.visibleIf(PropertyComponentPredicate(validationState, ValidationState.VALIDATED))

    row {
      icon(AllIcons.General.BalloonError).gap(RightGap.SMALL)
      label("").bindText(validationError)
    }.visibleIf(PropertyComponentPredicate(validationState, ValidationState.ERROR))

    row {
      icon(AllIcons.General.Warning).gap(RightGap.SMALL)
      label("Invalid input")
    }.visibleIf(PropertyComponentPredicate(validationState, ValidationState.INVALID_INPUT))
  }

  override fun afterBuildUi() {
    if (liveValidation.get()) {
      validateSchema()
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun validateSchema() {
    schemaErrorHolder.clear()
    dataErrorHolder.clear()

    val schema: JsonSchema? = try {
      val schemaNode = objectMapper.readTree(schemaEditor.text)
      JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode)).getSchema(schemaNode)
    } catch (e: Exception) {
      schemaErrorHolder.add(e)
      validationState.set(ValidationState.INVALID_INPUT)
      null
    }

    val dataNode: JsonNode? = try {
      objectMapper.readTree(dataEditor.text)
    } catch (e: Exception) {
      dataErrorHolder.add(e)
      validationState.set(ValidationState.INVALID_INPUT)
      null
    }

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `errorHolder`.
    validate()

    if (schema != null && dataNode != null) {
      val errors = schema.validate(dataNode)
      if (errors.isEmpty()) {
        validationState.set(ValidationState.VALIDATED)
      }
      else {
        validationState.set(ValidationState.ERROR)
        validationError.set(
          """
            <html>
            Data does not match schema:<br />
              ${errors.joinToString(separator = "<br />") { "- $it" }}
            </html>
          """.trimIndent()
        )
      }
    }
  }

  private fun createSchemaEditor() =
    DeveloperToolEditor(
      id = "schema",
      context = context,
      configuration = configuration,
      project = project,
      title = "JSON schema",
      editorMode = INPUT,
      parentDisposable = parentDisposable,
      initialLanguage = JsonLanguage.INSTANCE,
      textProperty = schemaText,
    ).apply {
      onTextChangeFromUi {
        if (liveValidation.get()) {
          validateSchema()
        }
      }
    }

  private fun createDataEditor() =
    DeveloperToolEditor(
      id = "data",
      context = context,
      configuration = configuration,
      project = project,
      title = "JSON data",
      editorMode = INPUT,
      parentDisposable = parentDisposable,
      initialLanguage = JsonLanguage.INSTANCE,
      textProperty = dataText,
    ).apply {
      onTextChangeFromUi {
        if (liveValidation.get()) {
          validateSchema()
        }
      }
    }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class ValidationState {

    VALIDATED,
    ERROR,
    INVALID_INPUT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<JsonSchemaValidator> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "JSON Schema",
      contentTitle = "JSON Schema Validator"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> JsonSchemaValidator) =
      { configuration -> JsonSchemaValidator(context, configuration, parentDisposable, project) }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val objectMapper = ObjectMapper()

    private val EXAMPLE_SCHEMA = """
{
  "${'$'}id": "https://example.com/person.schema.json",
  "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Person",
  "type": "object",
  "properties": {
    "firstName": {
      "type": "string",
      "description": "The person's first name."
    },
    "lastName": {
      "type": "string",
      "description": "The person's last name."
    },
    "age": {
      "description": "Age in years which must be equal to or greater than zero.",
      "type": "integer",
      "minimum": 0
    }
  }
}
    """.trimIndent()
    private val EXAMPLE_DATA = """
{
  "firstName": "John",
  "lastName": "Doe",
  "age": 21
}
    """.trimIndent()
  }
}