package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

class Notes(
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  private val project: Project?,
) : DeveloperUiTool(parentDisposable = parentDisposable) {
  // -- Properties ---------------------------------------------------------- //

  private val text = configuration.register("test", "", INPUT)

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
        cell(
            AdvancedEditor(
                id = "content",
                context = context,
                configuration = configuration,
                project = project,
                editorMode = AdvancedEditor.EditorMode.INPUT,
                parentDisposable = parentDisposable,
                textProperty = text,
              )
              .component
          )
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<Notes> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("notes.menu-title"),
        contentTitle = UiToolsBundle.message("notes.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> Notes) = { configuration ->
      Notes(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
