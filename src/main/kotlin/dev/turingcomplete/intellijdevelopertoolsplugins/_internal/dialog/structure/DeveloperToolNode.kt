package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog.structure

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService

internal class DeveloperToolNode(
  private val developerToolId: String,
  val parentDisposable: Disposable,
  val project: Project?,
  val developerToolContext: DeveloperToolContext,
  private val developerToolCreator: (DeveloperToolConfiguration) -> DeveloperTool
) : ContentNode(
  id = developerToolId,
  title = developerToolContext.menuTitle,
  toolTipText = developerToolContext.contentTitle
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

  fun destroyDeveloperToolInstance(developerTool: DeveloperTool) {
    _developerTools.find { it.instance === developerTool }?.let {
      DeveloperToolsPluginService.instance.removeDeveloperToolConfiguration(
        developerToolId = developerToolId,
        developerToolConfiguration = it.configuration
      )
      Disposer.dispose(developerTool)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun restoreDeveloperToolInstances(): List<DeveloperToolContainer> =
    DeveloperToolsPluginService.instance
      .getDeveloperToolConfigurations(developerToolId)
      .map { developerToolConfiguration -> doCreateNewDeveloperToolInstance(developerToolConfiguration) }

  private fun doCreateNewDeveloperToolInstance(
    developerToolConfiguration: DeveloperToolConfiguration = DeveloperToolsPluginService.instance.createDeveloperToolConfiguration(developerToolId)
  ): DeveloperToolContainer {
    val developerTool = developerToolCreator(developerToolConfiguration)
    Disposer.register(parentDisposable, developerTool)
    return DeveloperToolContainer(developerTool, developerToolConfiguration, developerToolContext)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class DeveloperToolContainer(
    val instance: DeveloperTool,
    val configuration: DeveloperToolConfiguration,
    val context: DeveloperToolContext
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}