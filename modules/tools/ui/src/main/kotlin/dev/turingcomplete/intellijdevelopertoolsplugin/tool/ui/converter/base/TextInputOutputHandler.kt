package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool.Companion.DUMMY_DIALOG_VALIDATION_REQUESTOR
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor.EditorMode.INPUT_OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import javax.swing.JComponent

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
) :
  InputOutputHandler(
    id = id,
    title = UiToolsBundle.message("converter.text-input-output-handler.title"),
    errorHolder = ErrorHolder(),
    liveConversionSupported = true,
    textDiffSupported = true,
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var inputOutputText = configuration.register("${id}Text", defaultText, INPUT)

  private lateinit var advancedEditor: AdvancedEditor

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun createComponent(): JComponent = panel {
    advancedEditor =
      AdvancedEditor(
          id = id,
          title = null,
          editorMode = INPUT_OUTPUT,
          parentDisposable = parentDisposable,
          configuration = configuration,
          context = context,
          project = project,
          textProperty = inputOutputText,
          diffSupport = diffSupport,
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

  override fun read(): ByteArray = inputOutputText.get().toByteArray()

  override fun write(output: ByteArray) {
    ApplicationManager.getApplication().invokeLater { inputOutputText.set(String(output)) }
  }

  fun setLanguage(language: Language) {
    advancedEditor.language = language
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
