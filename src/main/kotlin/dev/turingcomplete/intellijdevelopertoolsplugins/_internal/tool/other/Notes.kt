package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.other

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor

class Notes(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperTool(parentDisposable = parentDisposable) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val text = configuration.register("test", "", INPUT)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      cell(
        DeveloperToolEditor(
          editorMode = DeveloperToolEditor.EditorMode.INPUT,
          parentDisposable = parentDisposable,
          textProperty = text
        ).component
      ).align(Align.FILL).resizableColumn()
    }.resizableRow()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<Notes> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "Notes",
      contentTitle = "Notes"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> Notes) = { configuration -> Notes(configuration, parentDisposable) }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}