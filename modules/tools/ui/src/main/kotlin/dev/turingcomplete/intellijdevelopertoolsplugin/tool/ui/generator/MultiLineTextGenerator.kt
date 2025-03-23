package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.generator

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor

abstract class MultiLineTextGenerator(
  private val generatedTextTitle: String,
  private val context: DeveloperUiToolContext,
  private val project: Project?,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperUiTool(parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties ---------------------------------------------------------- //

  private val generatedTextEditor: AdvancedEditor by lazy { createGeneratedTextEditor() }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  final override fun Panel.buildUi() {
    buildConfigurationUi()

    row {
      button("Regenerate") { doGenerate() }
    }

    row {
      cell(generatedTextEditor.component).align(Align.FILL)
    }.resizableRow()
  }

  override fun afterBuildUi() {
    doGenerate()
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
    configuration.addChangeListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  override fun doDispose() {
    configuration.removeChangeListener(this)
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun doGenerate() {
    if (validate().isEmpty()) {
      generatedTextEditor.apply {
        text = generate()
      }
    }
  }

  private fun createGeneratedTextEditor() = AdvancedEditor(
    id = "generated-text",
    title = generatedTextTitle,
    editorMode = AdvancedEditor.EditorMode.OUTPUT,
    parentDisposable = parentDisposable,
    project = project,
    context = context,
    configuration = configuration
  )

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
