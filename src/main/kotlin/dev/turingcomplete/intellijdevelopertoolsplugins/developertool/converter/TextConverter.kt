package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.INPUT_OUTPUT
import java.awt.event.ItemEvent
import javax.swing.JComponent

abstract class TextConverter(
        id: String,
        title: String,
        private val convertActionTitle: String,
        private val revertActionTitle: String,
        private val sourceTitle: String,
        private val targetTitle: String,
        description: String? = null,
) : DeveloperTool(id = id, title = title, description = description) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var converterMode by createProperty("converterMode", ConverterMode.LIVE)

  private var lastActiveInput: DeveloperToolEditor? = null
  private var currentActiveInput: DeveloperToolEditor? = null

  private lateinit var sourceEditor: DeveloperToolEditor
  private lateinit var targetEditor: DeveloperToolEditor

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi(project: Project?, parentDisposable: Disposable) {
    sourceEditor = createEditor(id = id, title = sourceTitle) { doToTarget(it) }
    targetEditor = createEditor(id = id, title = targetTitle) { doToSource(it) }

    row {
      resizableRow()
      cell(sourceEditor.createComponent(parentDisposable)).align(Align.FILL)
    }

    buildConfigurationUi(project, parentDisposable)

    row {
      cell(createActionsComponent()).horizontalAlign(HorizontalAlign.FILL)
    }

    row {
      resizableRow()
      cell(targetEditor.createComponent(parentDisposable)).align(Align.FILL)
    }
  }

  protected open fun Panel.buildConfigurationUi(project: Project?, parentDisposable: Disposable) {}

  abstract fun toTarget(text: String): String

  abstract fun toSource(text: String): String

  protected fun transformToTarget() {
    doToTarget(sourceEditor.text)
  }

  protected fun transformToSource() {
    doToSource(targetEditor.text)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createActionsComponent(): JComponent = panel {
    buttonsGroup {
      row {
        radioButton("Live conversion").configure(ConverterMode.LIVE)
        val manualRadioButton = radioButton("Manual:").configure(ConverterMode.MANUAL).gap(RightGap.SMALL)
        button("▼ $convertActionTitle") { transformToTarget() }.enabledIf(manualRadioButton.selected).gap(RightGap.SMALL)
        button("▲ $revertActionTitle") { transformToSource() }.enabledIf(manualRadioButton.selected)
      }
    }
  }

  private fun doToTarget(text: String) {
    try {
      targetEditor.text = toTarget(text)
    }
    catch (ignore: Exception) {
    }
  }

  private fun doToSource(text: String) {
    try {
      sourceEditor.text = toSource(text)
    }
    catch (ignore: Exception) {
    }
  }

  private fun createEditor(id: String, title: String, onTextChange: (String) -> Unit) =
    DeveloperToolEditor(id = id, title = title, editorMode = INPUT_OUTPUT).apply {
      onFocusGained {
        currentActiveInput = this
        lastActiveInput = this

      }
      onFocusLost {
        currentActiveInput = null
      }
      onTextChange { nextText ->
        if (converterMode == ConverterMode.LIVE) {
          if (currentActiveInput != this) {
            // The text change was triggered by an action, but the focus grab
            // will be done asynchronously, so this editor does not have the
            // focus yet.
            currentActiveInput = this
            lastActiveInput = this
          }
          onTextChange(nextText)
        }
      }
    }

  private fun Cell<JBRadioButton>.configure(value: ConverterMode): Cell<JBRadioButton> {
    return this.apply {
      component.isSelected = converterMode == value
      component.addItemListener { event ->
        if (event.stateChange == ItemEvent.SELECTED) {
          switchConverterMode(value)
        }
      }
    }
  }

  private fun switchConverterMode(value: ConverterMode) {
    if (converterMode == value) {
      return
    }

    converterMode = value

    if (value == ConverterMode.LIVE) {
      // Trigger a text change, so if the text was changed in manual mode it
      // will now be converted once during the switch to live mode.
      lastActiveInput?.let {
        it.text = it.text
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  protected enum class ConverterMode {

    LIVE,
    MANUAL
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}

