package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.testfixtures

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.application
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ifNotEmpty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactoryEp
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.Converter

object DeveloperUiToolsInstances {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun createDeveloperUiToolsUnderTest(
    project: Project,
    parentDisposable: Disposable,
    settings: DeveloperToolsInstanceSettings,
  ): List<DeveloperUiToolUnderTest<*>> {
    val developerUiToolsUnderTest = mutableListOf<DeveloperUiToolUnderTest<*>>()

    DeveloperUiToolFactoryEp.EP_NAME.forEachExtensionSafe { factoryEp ->
      val configuration =
        settings.getDeveloperToolConfigurations(factoryEp.id).ifNotEmpty { first() }
          ?: settings.createDeveloperToolConfiguration(factoryEp.id)

      val context = DeveloperUiToolContext(factoryEp.id, false)
      val instance: DeveloperUiTool =
        factoryEp
          .createInstance(application)
          .getDeveloperUiToolCreator(project, parentDisposable, context)
          ?.invoke(configuration)
          ?: error("No instance of tool `${factoryEp.id}` was be created by its factory")
      configuration.wasConsumedByDeveloperTool = true
      runInEdtAndWait {
        instance.createComponent()
        instance.activated()
      }

      val developerUiToolUnderTest =
        when {
          instance is Converter ->
            ConverterDeveloperUiToolUnderTest(
              factoryEp = factoryEp,
              configuration = configuration,
              instance = instance,
            )

          else ->
            DeveloperUiToolUnderTest(
              factoryEp = factoryEp,
              configuration = configuration,
              instance = instance,
            )
        }
      developerUiToolsUnderTest.add(developerUiToolUnderTest)
    }

    return developerUiToolsUnderTest
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
