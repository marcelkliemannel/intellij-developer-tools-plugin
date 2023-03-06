package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator

import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor

abstract class MultiLineTextGenerator(
  presentation: DeveloperToolPresentation,
  private val generatedTextTitle: String,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperTool(presentation, parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val generatedTextEditor: DeveloperToolEditor by lazy { createGeneratedTextEditor() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  final override fun Panel.buildUi() {
    buildConfigurationUi()

    row {
      button("Regenerate") { doGenerate() }
    }

    row {
      cell(generatedTextEditor.createComponent()).align(Align.FILL)
    }.resizableRow()
  }

  protected abstract fun generate(): String

  open fun Panel.buildConfigurationUi() {
    // Override if needed
  }

  override fun configurationChanged(key: String) {
    if (!isDisposed) {
      doGenerate()
    }
  }

  override fun activated() {
    doGenerate()
    configuration.addChangeListener(this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  override fun doDispose() {
    configuration.removeChangeListener(this)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun doGenerate() {
    if (validate().isEmpty()) {
      generatedTextEditor.apply {
        text = generate()
      }
    }
  }

  private fun createGeneratedTextEditor() = DeveloperToolEditor(
    title = generatedTextTitle,
    editorMode = DeveloperToolEditor.EditorMode.OUTPUT,
    parentDisposable = parentDisposable
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}

