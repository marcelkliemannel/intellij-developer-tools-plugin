package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsInstanceSettings

internal class DeveloperToolNode(
  private val developerToolId: String,
  val parentDisposable: Disposable,
  val project: Project?,
  val settings: DeveloperToolsInstanceSettings,
  val developerUiToolPresentation: DeveloperUiToolPresentation,
  private val developerUiToolCreator: (DeveloperToolConfiguration) -> DeveloperUiTool
) : ContentNode(
  id = developerToolId,
  title = developerUiToolPresentation.menuTitle,
  toolTipText = developerUiToolPresentation.contentTitle
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val _developerTools: MutableList<DeveloperToolContainer> by lazy {
    restoreDeveloperToolInstances().ifEmpty { listOf(doCreateNewDeveloperToolInstance()) }.toMutableList()
  }
  val developerTools: List<DeveloperToolContainer>
    get() = _developerTools

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun createNewDeveloperToolInstance(): DeveloperToolContainer {
    val developerToolContainer = doCreateNewDeveloperToolInstance()
    _developerTools.add(developerToolContainer)
    return developerToolContainer
  }

  fun destroyDeveloperToolInstance(developerUiTool: DeveloperUiTool) {
    _developerTools.find { it.instance === developerUiTool }?.let {
      settings.removeDeveloperToolConfiguration(
        developerToolId = developerToolId,
        developerToolConfiguration = it.configuration
      )
      Disposer.dispose(developerUiTool)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun restoreDeveloperToolInstances(): List<DeveloperToolContainer> =
    settings.getDeveloperToolConfigurations(developerToolId)
      .map { developerToolConfiguration -> doCreateNewDeveloperToolInstance(developerToolConfiguration) }

  private fun doCreateNewDeveloperToolInstance(
    developerToolConfiguration: DeveloperToolConfiguration = settings.createDeveloperToolConfiguration(developerToolId)
  ): DeveloperToolContainer {
    val developerTool = developerUiToolCreator(developerToolConfiguration)
    developerToolConfiguration.wasConsumedByDeveloperTool = true
    Disposer.register(parentDisposable, developerTool)
    return DeveloperToolContainer(developerTool, developerToolConfiguration, developerUiToolPresentation)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class DeveloperToolContainer(
    val instance: DeveloperUiTool,
    val configuration: DeveloperToolConfiguration,
    val context: DeveloperUiToolPresentation
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}