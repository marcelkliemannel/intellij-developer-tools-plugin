package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty

abstract class MultiLineTextGenerator(
  private val generatedTextTitle: String,
  private val context: DeveloperToolContext,
  private val project: Project?,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperTool(parentDisposable), DeveloperToolConfiguration.ChangeListener {
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
      cell(generatedTextEditor.component).align(Align.FILL)
    }.resizableRow()
  }

  protected abstract fun generate(): String

  open fun Panel.buildConfigurationUi() {
    // Override if needed
  }

  override fun configurationChanged(property: ValueProperty<out Any>) {
    if (!isDisposed) {
      doGenerate()
    }
  }

  override fun activated() {
    doGenerate()
    configuration.addChangeListener(parentDisposable, this)
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
    id = "generated-text",
    title = generatedTextTitle,
    editorMode = DeveloperToolEditor.EditorMode.OUTPUT,
    parentDisposable = parentDisposable,
    project = project,
    context = context,
    configuration = configuration
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}

