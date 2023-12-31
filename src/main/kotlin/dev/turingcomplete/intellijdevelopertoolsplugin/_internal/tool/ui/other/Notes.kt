package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolExContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor

class Notes(
  private val context: DeveloperUiToolExContext,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  private val project: Project?
) : DeveloperUiTool(parentDisposable = parentDisposable) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val text = configuration.register("test", "", INPUT)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      cell(
        DeveloperToolEditor(
          id = "content",
          context = context,
          configuration = configuration,
          project = project,
          editorMode = DeveloperToolEditor.EditorMode.INPUT,
          parentDisposable = parentDisposable,
          textProperty = text
        ).component
      ).align(Align.FILL).resizableColumn()
    }.resizableRow()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<Notes> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Notes",
      contentTitle = "Notes"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolExContext
    ): ((DeveloperToolConfiguration) -> Notes) = { configuration -> Notes(context, configuration, parentDisposable, project) }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}