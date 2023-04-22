package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.INPUT_OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.TextConverter.ActiveInput.SOURCE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.TextConverter.ActiveInput.TARGET
import kotlin.properties.Delegates

internal abstract class TextConverter(
  protected val textConverterContext: TextConverterContext,
  protected val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperTool(parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var liveConversion = configuration.register("liveConversion", true)
  private val conversationAlarm by lazy { Alarm(parentDisposable) }

  private var lastActiveInput: ActiveInput? = null

  private val sourceEditor by lazy { createEditor(SOURCE, textConverterContext.sourceTitle) { doToTarget(it) } }
  private val targetEditor by lazy { createEditor(TARGET, textConverterContext.targetTitle) { doToSource(it) } }

  protected var sourceText: String by Delegates.observable("") { _, _, new -> sourceEditor.text = new }
  protected var targetText: String by Delegates.observable("") { _, _, new -> targetEditor.text = new }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    liveConversion.afterChange(parentDisposable) {
      handleLiveConversionSwitch()
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    buildTopConfigurationUi()

    row {
      resizableRow()
      val sourceEditorCell = cell(sourceEditor.createComponent()).align(Align.FILL)
      textConverterContext.sourceErrorHolder?.let { sourceErrorHolder ->
        sourceEditorCell.validationOnApply(sourceEditor.bindValidator(sourceErrorHolder.asValidation()))
      }
    }

    buildMiddleFirstConfigurationUi()
    buildActionsUi()
    buildMiddleSecondConfigurationUi()

    row {
      resizableRow()
      val targetEditorCell = cell(targetEditor.createComponent()).align(Align.FILL)
      textConverterContext.targetErrorHolder?.let { targetErrorHolder ->
        targetEditorCell.validationOnApply(targetEditor.bindValidator(targetErrorHolder.asValidation()))
      }
    }
  }

  protected open fun Panel.buildTopConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildMiddleFirstConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildMiddleSecondConfigurationUi() {
    // Override if needed
  }

  protected fun setSourceLanguage(language: Language) {
    sourceEditor.language = language
  }

  protected fun setTargetLanguage(language: Language) {
    targetEditor.language = language
  }

  abstract fun toTarget(text: String)

  abstract fun toSource(text: String)

  override fun configurationChanged() {
    transformToTarget()
  }

  override fun activated() {
    configuration.addChangeListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Panel.buildActionsUi() {
    buttonsGroup {
      row {
        val liveConversionCheckBox = checkBox("Live conversion")
          .bindSelected(liveConversion)
          .gap(RightGap.SMALL)

        button("▼ ${textConverterContext.convertActionTitle}") { transformToTarget() }
          .enabledIf(liveConversionCheckBox.selected.not())
          .gap(RightGap.SMALL)
        button("▲ ${textConverterContext.revertActionTitle}") { transformToSource() }
          .enabledIf(liveConversionCheckBox.selected.not())
      }
    }
  }

  private fun transformToSource() {
    doToSource(targetEditor.text)
  }

  private fun transformToTarget() {
    doToTarget(sourceEditor.text)
  }

  private fun doToTarget(text: String) {
    doConversation {
      try {
        toTarget(text)
      }
      catch (ignore: Exception) {
      }
    }
  }

  private fun doToSource(text: String) {
    doConversation {
      try {
        toSource(text)
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
        if (liveConversion.get()) {
          lastActiveInput = activeInput
          onTextChange(text)
        }
      }
    }

  private fun handleLiveConversionSwitch() {
    if (liveConversion.get()) {
      // Trigger a text change. So if the text was changed in manual mode, it
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

  data class TextConverterContext(
    val convertActionTitle: String,
    val revertActionTitle: String,
    val sourceTitle: String,
    val targetTitle: String,
    val sourceErrorHolder: ErrorHolder? = null,
    val targetErrorHolder: ErrorHolder? = null
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}

