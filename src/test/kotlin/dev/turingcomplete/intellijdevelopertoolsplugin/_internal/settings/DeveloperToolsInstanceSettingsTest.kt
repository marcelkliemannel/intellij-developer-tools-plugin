package dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.junit5.RunInEdt
import com.intellij.testFramework.junit5.RunMethodInEdt
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.ui.JBColor
import com.intellij.util.application
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.DeveloperUiToolFactoryEp
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZoneId
import java.util.*
import kotlin.random.Random

@RunInEdt(allMethods = false)
@TestApplication
class DeveloperToolsInstanceSettingsTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val fixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("test").fixture
  private val disposable = Disposable { }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @BeforeEach
  fun beforeEach() {
    fixture.setUp()
  }

  @AfterEach
  @RunMethodInEdt(writeIntent = RunMethodInEdt.WriteIntentMode.True)
  fun afterEach() {
    Disposer.dispose(disposable)
    fixture.tearDown()
  }

  @Test
  fun `Check various configurations persistent states`() {
    IdeaTestFixtureFactory.getFixtureFactory().createLightFixtureBuilder("dd").fixture

    DeveloperToolsApplicationSettings.instance.also {
      it.saveConfigurations = true
      it.saveInputs = true
      it.saveSensitiveInputs = true
    }

    val settings = object : DeveloperToolsInstanceSettings() {
      init {
        loadState(InstanceState())
      }
    }

    val developerUiTools = instantiateAllDeveloperUiTools(settings)

    // Delete all example values
    DeveloperToolsApplicationSettings.instance.loadExamples = false
    resetAllConfigurations(developerUiTools, settings, false)

    // Expect: No configurations have persisted because there are no property changes
    // (The persistent state will only include configuration with modified properties)
    assertThat(settings.getState().developerToolsConfigurations).isEmpty()

    // Set the values to the example value, despite `loadExamples` is disabled
    modifyAllConfigurationProperties(developerUiTools, settings) { property ->
      property.example?.let { exampleProvider ->
        property.reference.setWithUncheckedCast(exampleProvider(), null)
      }
    }

    // Expect: No configurations have persisted because there are no property changes
    // (Changes in the `loadExamples` setting are not reflected in the existing
    // configurations until the application was restarted. Therefore, during the
    // modification check, we will always check if the value is equal to the
    // example.)
    assertThat(settings.getState().developerToolsConfigurations).isEmpty()

    // Set the values to the default value
    modifyAllConfigurationProperties(developerUiTools, settings) { property ->
      property.reference.setWithUncheckedCast(property.defaultValue, null)
    }

    // Expect: No configurations have persisted because there are no property changes
    assertThat(settings.getState().developerToolsConfigurations).isEmpty()

    // Load all example values
    DeveloperToolsApplicationSettings.instance.loadExamples = true
    resetAllConfigurations(developerUiTools, settings, true)

    // Expect: No configurations have persisted because there are no property changes
    assertThat(settings.getState().developerToolsConfigurations).isEmpty()

    // Set the values to a random value
    modifyAllConfigurationProperties(developerUiTools, settings) { property ->
      val randomValue = if (property.key == "timeZoneId") {
        val availableZoneIds = ZoneId.getAvailableZoneIds().toList()
        availableZoneIds[Random.nextInt(0, availableZoneIds.size - 1)]
      }
      else {
        when (property.defaultValue) {
          is String -> property.defaultValue + "foo"
          is Int -> property.defaultValue + 1
          is Long -> property.defaultValue + 1
          is BigDecimal -> property.defaultValue.plus(BigDecimal.ONE)
          is Boolean -> !property.defaultValue
          is LocaleContainer -> {
            val availableLocales = Locale.getAvailableLocales()
            LocaleContainer(availableLocales[Random.nextInt(0, availableLocales.size - 1)])
          }

          is Enum<*> -> {
            val enumConstants = property.defaultValue::class.java.enumConstants
            enumConstants[(property.defaultValue.ordinal + 1) % enumConstants.size]
          }

          is JBColor -> JBColor(Random.nextInt(0, 255), Random.nextInt(0, 255))
          else -> throw IllegalStateException("Missing property type mapping for: " + property.defaultValue::class)
        }
      }
      property.reference.setWithUncheckedCast(randomValue, null)
    }

    // Expect: All configurations (with properties) have been persisted
    assertThat(settings.getState().developerToolsConfigurations)
      .hasSize(developerUiTools.filter {
        settings.getDeveloperToolConfigurations(it.developerToolFactoryEp.id)[0].properties.isNotEmpty()
      }.size)
  }

  private fun resetAllConfigurations(
    developerUiTools: List<DeveloperUiToolWrapper<*>>,
    settings: DeveloperToolsInstanceSettings,
    loadExamples: Boolean
  ) {
    developerUiTools.forEach { (developerUiToolEp, _, _) ->
      settings.getDeveloperToolConfigurations(developerUiToolEp.id).forEach {
        ApplicationManager.getApplication().invokeAndWait({
          it.reset(null, loadExamples)
        }, ModalityState.any())
      }
    }
  }

  private fun modifyAllConfigurationProperties(
    developerUiTools: List<DeveloperUiToolWrapper<*>>,
    settings: DeveloperToolsInstanceSettings,
    modify: (DeveloperToolConfiguration.PropertyContainer) -> Unit
  ) {
    developerUiTools.forEach { (developerUiToolEp, _) ->
      settings.getDeveloperToolConfigurations(developerUiToolEp.id).forEach {
        ApplicationManager.getApplication().invokeAndWait({
          it.properties.values.forEach { property ->
            modify(property)
          }
        }, ModalityState.any())
      }
    }
  }

  private fun instantiateAllDeveloperUiTools(settings: DeveloperToolsInstanceSettings): List<DeveloperUiToolWrapper<*>> {
    val developerUiTools = mutableListOf<DeveloperUiToolWrapper<*>>()

    DeveloperUiToolFactoryEp.EP_NAME.forEachExtensionSafe { developerToolFactoryEp ->
      val developerUiToolFactory: DeveloperUiToolFactory<*> = developerToolFactoryEp.createInstance(application)
      val context = DeveloperUiToolContext(developerToolFactoryEp.id, false)
      val developerToolConfiguration = settings.createDeveloperToolConfiguration(developerToolFactoryEp.id)
      val developerUiTool = developerUiToolFactory
        .getDeveloperUiToolCreator(fixture.project, disposable, context)
        ?.invoke(developerToolConfiguration)
      if (developerUiTool != null) {
        developerToolConfiguration.wasConsumedByDeveloperTool = true
        ApplicationManager.getApplication().invokeAndWait({
          developerUiTool.createComponent()
          developerUiTool.activated()
        }, ModalityState.any())
        developerUiTools.add(DeveloperUiToolWrapper(
          developerToolFactoryEp = developerToolFactoryEp,
          developerUiTool = developerUiTool,
          developerToolConfiguration = developerToolConfiguration
        ))
      }
      else {
        fail("No instance of tool was created: ${developerToolFactoryEp.id}")
      }
    }

    return developerUiTools
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private data class DeveloperUiToolWrapper<T : DeveloperUiTool>(
    val developerToolFactoryEp: DeveloperUiToolFactoryEp<out DeveloperUiToolFactory<*>>,
    val developerUiTool: T,
    val developerToolConfiguration: DeveloperToolConfiguration
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    @BeforeAll
    @JvmStatic
    fun beforeAll() {
      System.setProperty("java.awt.headless", false.toString())
    }

    @AfterAll
    @JvmStatic
    fun afterAll() {
      System.setProperty("java.awt.headless", false.toString())
    }
  }
}