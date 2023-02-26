package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.INPUT_OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.onSelectionChanged

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

  private var liveConversion by createProperty("liveConversion", true)
  private lateinit var conversationAlarm: Alarm

  private var lastActiveInput: DeveloperToolEditor? = null
  private var currentActiveInput: DeveloperToolEditor? = null

  private lateinit var sourceEditor: DeveloperToolEditor
  private lateinit var targetEditor: DeveloperToolEditor

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi(project: Project?, parentDisposable: Disposable) {
    conversationAlarm = Alarm(parentDisposable)

    sourceEditor = createEditor(id = id, title = sourceTitle) { doToTarget(it) }
    targetEditor = createEditor(id = id, title = targetTitle) { doToSource(it) }

    row {
      resizableRow()
      cell(sourceEditor.createComponent(parentDisposable)).align(Align.FILL)
    }

    buildConfigurationUi(project, parentDisposable)
    buildActionsUi()

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

  private fun transformToSource() {
    doToSource(targetEditor.text)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Panel.buildActionsUi() {
    buttonsGroup {
      row {
        val liveConversionCheckBox = checkBox("Live conversion").applyToComponent {
          isSelected = liveConversion
          onSelectionChanged { switchLiveConversion(it) }
        }.gap(RightGap.SMALL)

        button("▼ $convertActionTitle") { transformToTarget() }.enabledIf(liveConversionCheckBox.selected.not()).gap(RightGap.SMALL)
        button("▲ $revertActionTitle") { transformToSource() }.enabledIf(liveConversionCheckBox.selected.not())
      }
    }
  }

  private fun doToTarget(text: String) {
    doConversation {
      try {
        targetEditor.text = toTarget(text)
      }
      catch (ignore: Exception) {
      }
    }
  }

  private fun doToSource(text: String) {
    doConversation {
      try {
        sourceEditor.text = toSource(text)
      }
      catch (ignore: Exception) {
      }
    }
  }

  private fun doConversation(conversation: () -> Unit) {
    conversationAlarm.cancelAllRequests()
    conversationAlarm.addRequest(conversation, 0)
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
        if (liveConversion) {
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

  private fun switchLiveConversion(value: Boolean) {
    if (liveConversion == value) {
      return
    }

    liveConversion = value

    if (liveConversion) {
      // Trigger a text change, so if the text was changed in manual mode it
      // will now be converted once during the switch to live mode.
      lastActiveInput?.let {
        it.text = it.text
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}

