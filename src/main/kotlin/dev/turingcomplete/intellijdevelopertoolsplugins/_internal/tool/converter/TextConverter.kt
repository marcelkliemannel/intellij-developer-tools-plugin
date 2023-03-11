package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.INPUT_OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.TextConverter.ActiveInput.SOURCE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.TextConverter.ActiveInput.TARGET

internal abstract class TextConverter(
        presentation: DeveloperToolPresentation,
        private val context: Context,
        protected val configuration: DeveloperToolConfiguration,
        parentDisposable: Disposable
) : DeveloperTool(presentation, parentDisposable) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var liveConversion: Boolean by configuration.register("liveConversion", true)
  private val conversationAlarm by lazy { Alarm(parentDisposable) }

  private var lastActiveInput: ActiveInput? = null

  private val sourceEditor by lazy { createEditor(SOURCE, context.sourceTitle) { doToTarget(it) } }
  private val targetEditor by lazy { createEditor(TARGET, context.targetTitle) { doToSource(it) } }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      resizableRow()
      cell(sourceEditor.createComponent()).align(Align.FILL)
    }

    buildConfigurationUi()
    buildActionsUi()

    row {
      resizableRow()
      cell(targetEditor.createComponent()).align(Align.FILL)
    }
  }

  protected open fun Panel.buildConfigurationUi() {
    // Override if needed
  }

  abstract fun toTarget(text: String): String

  abstract fun toSource(text: String): String

  protected fun transformToTarget() {
    doToTarget(sourceEditor.text)
  }

  private fun transformToSource() {
    doToSource(targetEditor.text)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  private fun Panel.buildActionsUi() {
    buttonsGroup {
      row {
        val liveConversionCheckBox = checkBox("Live conversion")
                .applyToComponent { isSelected = liveConversion }
                .whenStateChangedFromUi { switchLiveConversion(it) }
                .gap(RightGap.SMALL)

        button("▼ ${context.convertActionTitle}") { transformToTarget() }
                .enabledIf(liveConversionCheckBox.selected.not())
                .gap(RightGap.SMALL)
        button("▲ ${context.revertActionTitle}") { transformToSource() }
                .enabledIf(liveConversionCheckBox.selected.not())
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

  private fun createEditor(activeInput: ActiveInput, title: String, onTextChange: (String) -> Unit) =
    DeveloperToolEditor(title = title, editorMode = INPUT_OUTPUT, parentDisposable = parentDisposable).apply {
      onFocusGained {
        lastActiveInput = activeInput
      }
      this.onTextChangeFromUi { text ->
        if (liveConversion) {
          lastActiveInput = activeInput
          onTextChange(text)
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
      when (lastActiveInput) {
        SOURCE -> transformToTarget()
        TARGET -> transformToSource()
        null -> {}
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class ActiveInput {

    SOURCE,
    TARGET
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class Context(
          val convertActionTitle: String,
          val revertActionTitle: String,
          val sourceTitle: String,
          val targetTitle: String
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}

