package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.onSelected

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

  init {
    registerPropertyChangeListeners {
      if (transformerMode == TransformerMode.LIVE) {
        doTransform()
      }
    }
  }

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

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun doTransform() {
    resultEditor.text = transform(sourceEditor.text)
  }

  private fun createActionsComponent() = panel {
    buttonsGroup {
      row {
        radioButton("Live transformation").configure(TransformerMode.LIVE)
        val manualRadioButton = radioButton("Manual:").configure(TransformerMode.MANUAL).gap(RightGap.SMALL)
        button("▼ $transformActionTitle") { doTransform() }.enabledIf(manualRadioButton.selected).gap(RightGap.SMALL)
      }
    }
  }

  private fun Cell<JBRadioButton>.configure(value: TransformerMode) = this.applyToComponent {
    isSelected = transformerMode == value
    onSelected { transformerMode = value }
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
}