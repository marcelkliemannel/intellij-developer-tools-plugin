package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.OUTPUT
import kotlin.properties.Delegates

abstract class TextTransformer(
  presentation: DeveloperToolContext,
  private val context: Context,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperTool(presentation, parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  protected var liveTransformation = configuration.register("liveTransformation", true)

  private val sourceEditor: DeveloperToolEditor by lazy { createSourceInputEditor() }
  private val resultEditor by lazy { createResultOutputEditor(parentDisposable) }

  protected val sourceText: String
    get() = sourceEditor.text
  protected var resultText: String by Delegates.observable("") { _, _, new -> resultEditor.text = new }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  protected abstract fun transform()

  override fun Panel.buildUi() {
    buildTopConfigurationUi()

    row {
      cell(sourceEditor.createComponent()).align(Align.FILL)
    }.resizableRow()

    buildMiddleConfigurationUi()
    buildActionsUi()

    row {
      cell(resultEditor.createComponent()).align(Align.FILL)
    }.resizableRow()
  }

  override fun afterBuildUi() {
    transform()
  }

  protected open fun Panel.buildTopConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildMiddleConfigurationUi() {
    // Override if needed
  }

  protected open fun getInitialOriginalText(): String? {
    // Override if needed
    return null
  }

  protected open fun getInitialLanguage(): Language? {
    // Override if needed
    return null
  }

  protected fun setLanguage(language: Language) {
    sourceEditor.language = language
    resultEditor.language = language
  }

  override fun configurationChanged() {
    if (!isDisposed && liveTransformation.get()) {
      transform()
    }
  }

  override fun activated() {
    configuration.addChangeListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  override fun doDispose() {
    configuration.removeChangeListener(this)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Panel.buildActionsUi() {
    buttonsGroup {
      row {
        val liveTransformationCheckBox = checkBox("Live transformation")
          .bindSelected(liveTransformation)
          .gap(RightGap.SMALL)

        button("▼ ${context.transformActionTitle}") { transform() }
          .enabledIf(liveTransformationCheckBox.selected.not())
          .component
      }
    }
  }

  private fun createSourceInputEditor(): DeveloperToolEditor =
    DeveloperToolEditor(
      title = context.sourceTitle,
      editorMode = INPUT,
      parentDisposable = parentDisposable
    ).apply {
      getInitialLanguage()?.let { language = it }
      getInitialOriginalText()?.let { text = it }
      onTextChangeFromUi { _ ->
        if (liveTransformation.get()) {
          transform()
        }
      }
    }

  private fun createResultOutputEditor(parentDisposable: Disposable) =
    DeveloperToolEditor(
      title = context.resultTitle,
      editorMode = OUTPUT,
      parentDisposable = parentDisposable
    ).apply {
      getInitialLanguage()?.let { language = it }
    }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class Context(
    val transformActionTitle: String,
    val sourceTitle: String,
    val resultTitle: String
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}