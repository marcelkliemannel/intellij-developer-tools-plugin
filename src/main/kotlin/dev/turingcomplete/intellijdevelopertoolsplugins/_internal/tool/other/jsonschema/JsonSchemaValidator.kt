package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.other.jsonschema

import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ErrorHolder
import net.pwall.json.schema.codegen.CodeGenerator
import net.pwall.json.schema.codegen.TargetLanguage
import java.io.PrintWriter
import java.io.StringWriter


class JsonSchemaCodeGenerator(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperTool(
  developerToolContext = DeveloperToolContext(
    menuTitle = "Schema to Code Generator",
    contentTitle = "Schema to Code Generator"
  ),
  parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var codeLanguage = configuration.register("language", Language.JAVA)
  private var liveConversion = configuration.register("liveConversion", true)

  private val schemaEditor by lazy { this.createSchemaEditor() }
  private val schemaErrorHolder = ErrorHolder()
  private val codeEditor by lazy { this.createCodeEditor() }

  private val codeStyles by lazy { LanguageCodeStyleSettingsProvider.EP_NAME.extensionList.associate { it.language.id to it.language } }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    codeLanguage.afterChange {
      codeStyles[it.languageId]?.let { codeStyle -> codeEditor.language = codeStyle }
      transform()
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      cell(schemaEditor.createComponent()).align(Align.FILL)
        .validationOnApply(schemaEditor.bindValidator(schemaErrorHolder.asValidation()))
    }.resizableRow()

    row {
      comboBox(Language.values().toList())
        .bindItem(codeLanguage)
        .label("Generated code language:")
    }

    row {
      cell(codeEditor.createComponent()).align(Align.FILL)
    }.resizableRow()
  }

  override fun afterBuildUi() {
    transform()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun transform() {
    schemaErrorHolder.unset()

    try {
      val codeWriter = StringWriter()
      val codeGenerator = CodeGenerator().apply {
        targetLanguage = codeLanguage.get().targetLanguage
        baseDirectoryName = "output/directory"
        basePackageName = "com.example"

        outputResolver = { PrintWriter(codeWriter) }
      }
      val schema = codeGenerator.schemaParser.parse(schemaEditor.text)
      codeGenerator.generateClass(schema, "GeneratedClass")
      codeEditor.text = codeWriter.toString()
    } catch (e: Exception) {
      schemaErrorHolder.set(e)
    }

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `errorHolder`.
    validate()
  }

  private fun createSchemaEditor() =
    DeveloperToolEditor(
      title = "JSON Schema",
      editorMode = INPUT,
      parentDisposable = parentDisposable,
      initialLanguage = JsonLanguage.INSTANCE
    ).apply {
      text = DEFAULT_SCHEMA
      onTextChangeFromUi {
        if (liveConversion.get()) {
          transform()
        }
      }
    }

  private fun createCodeEditor() =
    DeveloperToolEditor(
      title = "Generated code",
      editorMode = OUTPUT,
      parentDisposable = parentDisposable,
      initialLanguage = codeStyles[codeLanguage.get().languageId] ?: PlainTextLanguage.INSTANCE
    )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class Language(val targetLanguage: TargetLanguage, val title: String, val languageId: String) {

    KOTLIN(TargetLanguage.KOTLIN, "Kotlin", "KOTLIN"),
    JAVA(TargetLanguage.JAVA, "Java", "JAVA"),
    TYPESCRIPT(TargetLanguage.TYPESCRIPT, "TypeScript", "TYPE_SCRIPT");

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<JsonSchemaCodeGenerator> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = JsonSchemaCodeGenerator(configuration, parentDisposable)
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_SCHEMA = """
{
  "${'$'}schema": "http://json-schema.org/draft/2019-09/schema",
  "${'$'}id": "http://pwall.net/test",
  "title": "Product",
  "type": "object",
  "required": ["id", "name", "price"],
  "properties": {
    "id": {
      "type": "number",
      "description": "Product identifier"
    },
    "name": {
      "type": "string",
      "description": "Name of the product"
    },
    "price": {
      "type": "number",
      "minimum": 0
    },
    "tags": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "stock": {
      "type": "object",
      "properties": {
        "warehouse": {
          "type": "number"
        },
        "retail": {
          "type": "number"
        }
      }
    }
  }
}
    """
  }
}