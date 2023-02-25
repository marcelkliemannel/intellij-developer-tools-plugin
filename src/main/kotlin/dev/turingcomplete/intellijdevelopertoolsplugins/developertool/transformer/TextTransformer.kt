package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.onSelectionChanged

abstract class TextTransformer(
        id: String,
        title: String,
        private val transformActionTitle: String,
        private val sourceTitle: String,
        private val resultTitle: String,
        description: String? = null,
) : DeveloperTool(id = id, title = title, description = description) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var liveTransformation: Boolean by createProperty("liveTransformation", true)
  private lateinit var transformationAlarm: Alarm

  private lateinit var sourceEditor: DeveloperToolEditor
  private lateinit var resultEditor: DeveloperToolEditor

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    registerPropertyChangeListeners {
      if (liveTransformation) {
        doTransform()
      }
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun transform(text: String): String

  override fun Panel.buildUi(project: Project?, parentDisposable: Disposable) {
    transformationAlarm = Alarm(parentDisposable)

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
    transformationAlarm.cancelAllRequests()
    transformationAlarm.addRequest({ resultEditor.text = transform(sourceEditor.text) }, 0)
  }

  private fun createActionsComponent() = panel {
    buttonsGroup {
      row {
        val liveTransformationCheckBox = checkBox("Live transformation").applyToComponent {
          isSelected = liveTransformation
          onSelectionChanged { liveTransformation = it }
        }

        button("▼ $transformActionTitle") { doTransform() }.enabledIf(liveTransformationCheckBox.selected.not())
      }
    }
  }

  private fun createSourceInputEditor(): DeveloperToolEditor =
    DeveloperToolEditor(id, sourceTitle, editorMode = INPUT).apply {
      onTextChange {
        if (liveTransformation) {
          doTransform()
        }
      }
    }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}