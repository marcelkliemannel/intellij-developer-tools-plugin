package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.textescape.TextEscape
import java.awt.event.ItemEvent

abstract class TextTransformer(
        id: String,
        title: String,
        private val transformActionTitle: String,
        private val sourceTitle: String,
        private val resultTitle: String,
        description: String? = null,
) : DeveloperTool(id = id, title = title, description = description) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var transformerMode by createProperty("transformerMode", TransformerMode.LIVE)

  private lateinit var sourceEditor: DeveloperToolEditor
  private lateinit var resultEditor: DeveloperToolEditor

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun transform(text: String): String

  override fun Panel.buildUi(project: Project?, parentDisposable: Disposable) {
    row {
      resizableRow()
      sourceEditor = createSourceInputEditor()
      cell(sourceEditor.createComponent(parentDisposable)).align(Align.FILL)
    }

    buildConfigurationUi(project, parentDisposable)

    row {
      cell(createActionsComponent()).horizontalAlign(HorizontalAlign.FILL)
    }

    row {
      resizableRow()
      resultEditor = DeveloperToolEditor(id = id, title = resultTitle, editorMode = OUTPUT)
      cell(resultEditor.createComponent(parentDisposable)).align(Align.FILL)
    }
  }

  protected open fun Panel.buildConfigurationUi(project: Project?, parentDisposable: Disposable) {}

  fun doTransform() {
    resultEditor.text = transform(sourceEditor.text)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createActionsComponent() = panel {
    buttonsGroup {
      row {
        radioButton("Live transformation").configure(TransformerMode.LIVE)
        val manualRadioButton = radioButton("Manual:").configure(TransformerMode.MANUAL).gap(RightGap.SMALL)
        button("▼ $transformActionTitle") { doTransform() }.enabledIf(manualRadioButton.selected).gap(RightGap.SMALL)
      }
    }
  }

  private fun Cell<JBRadioButton>.configure(value: TransformerMode): Cell<JBRadioButton> {
    return this.apply {
      component.isSelected = transformerMode == value
      component.addItemListener { event ->
        if (event.stateChange == ItemEvent.SELECTED) {
          transformerMode = value
        }
      }
    }
  }

  private fun createSourceInputEditor(): DeveloperToolEditor =
    DeveloperToolEditor(id, sourceTitle, editorMode = INPUT).apply {
      onTextChange {
        if (transformerMode == TransformerMode.LIVE) {
          doTransform()
        }
      }
    }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class TransformerMode {

    LIVE,
    MANUAL
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val EP: ExtensionPointName<TextEscape> = ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.textTransformer")
  }
}