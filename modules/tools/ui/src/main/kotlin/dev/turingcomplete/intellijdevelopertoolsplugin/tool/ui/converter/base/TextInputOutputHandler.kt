package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.toHexString
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool.Companion.DUMMY_DIALOG_VALIDATION_REQUESTOR
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor.EditorMode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

class TextInputOutputHandler(
  id: String,
  private val configuration: DeveloperToolConfiguration,
  private val context: DeveloperUiToolContext,
  private val parentDisposable: Disposable,
  private val project: Project?,
  private val focusGained: (TextInputOutputHandler) -> Unit,
  private val liveConversionRequested: (TextInputOutputHandler) -> Unit,
  private val diffSupport: AdvancedEditor.DiffSupport? = null,
  defaultText: String = "",
  exampleText: String? = null,
  inputOutputDirection: InputOutputDirection,
  private val initialLanguage: Language = PlainTextLanguage.INSTANCE,
  private val bytesToTextMode: BytesToTextMode = BytesToTextMode.BYTES_TO_CHARACTERS,
) :
  InputOutputHandler(
    id = id,
    title =
      when (bytesToTextMode) {
        BytesToTextMode.BYTES_TO_CHARACTERS ->
          UiToolsBundle.message("converter.text-input-output-handler.simple-title")
        BytesToTextMode.BYTES_TO_HEX ->
          UiToolsBundle.message("converter.text-input-output-handler.hex-title")
      },
    errorHolder = ErrorHolder(),
    liveConversionSupported = true,
    textDiffSupported = true,
    inputOutputDirection = inputOutputDirection,
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var inputOutputText =
    if (inputOutputDirection.supportsRead)
      configuration.register("${id}Text", defaultText, INPUT, exampleText)
    else ValueProperty("")

  private lateinit var advancedEditor: AdvancedEditor

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildUi() {
    advancedEditor =
      AdvancedEditor(
          id = id,
          title = null,
          editorMode =
            when (inputOutputDirection) {
              InputOutputDirection.UNDIRECTIONAL_WRITE -> EditorMode.OUTPUT
              InputOutputDirection.UNDIRECTIONAL_READ -> EditorMode.INPUT
              InputOutputDirection.BIDIRECTIONAL -> EditorMode.INPUT_OUTPUT
            },
          parentDisposable = parentDisposable,
          configuration = configuration,
          context = context,
          project = project,
          textProperty = inputOutputText,
          diffSupport = diffSupport,
          initialLanguage = initialLanguage,
        )
        .apply {
          onFocusGained { focusGained(this@TextInputOutputHandler) }
          onTextChangeFromUi {
            focusGained(this@TextInputOutputHandler)
            liveConversionRequested(this@TextInputOutputHandler)
          }
        }

    row {
        cell(advancedEditor.component)
          .validationOnApply(advancedEditor.bindValidator(errorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  override fun read(): ByteArray {
    check(inputOutputDirection.supportsRead)
    return inputOutputText.get().toByteArray()
  }

  override fun write(output: ByteArray) {
    check(inputOutputDirection.supportsWrite)
    val text =
      when (bytesToTextMode) {
        BytesToTextMode.BYTES_TO_CHARACTERS -> String(output)
        BytesToTextMode.BYTES_TO_HEX -> output.toHexString()
      }
    ApplicationManager.getApplication().invokeLater { inputOutputText.set(text) }
  }

  fun setLanguage(language: Language) {
    advancedEditor.language = language
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  enum class BytesToTextMode {

    BYTES_TO_CHARACTERS,
    BYTES_TO_HEX,
  }

  // -- Companion Object ---------------------------------------------------- //
}
