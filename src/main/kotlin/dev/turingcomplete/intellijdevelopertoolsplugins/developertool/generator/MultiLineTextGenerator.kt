package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.OUTPUT

abstract class MultiLineTextGenerator(id: String, title: String, description: String? = null) :
  DeveloperTool(id = id, title = title, description = description) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var generatedTextEditor: DeveloperToolEditor

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    registerPropertyChangeListeners { doGenerate() }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  final override fun Panel.buildUi(project: Project?, parentDisposable: Disposable) {
    buildConfigurationUi(project, parentDisposable)

    row {
      button("Regenerate") { doGenerate() }
    }

    row {
      generatedTextEditor = DeveloperToolEditor(id = id, title = title, editorMode = OUTPUT)
      cell(generatedTextEditor.createComponent(parentDisposable)).align(Align.FILL)
    }.resizableRow()
  }

  protected abstract fun generate(): String

  open fun Panel.buildConfigurationUi(project: Project?, parentDisposable: Disposable) {}

  override fun activated() {
    doGenerate()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun doGenerate() {
    if (validate().isEmpty()) {
      generatedTextEditor.apply {
        text = generate()
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}

