package dev.turingcomplete.intellijdevelopertoolsplugin.plugin

import IdeaTest
import PluginUtils.getPluginVersion
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.JDOMUtil
import com.intellij.ui.JBColor
import com.intellij.util.application
import com.intellij.util.xmlb.XmlSerializer
import dev.turingcomplete.intellijdevelopertoolsplugin.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.Companion.builtInConfigurationPropertyTypes
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.Companion.configurationPropertyTypesByNamesAndLegacyValueTypes
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.StatePropertyValueConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactoryEp
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.time.ZoneId
import java.util.*
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.writer
import kotlin.random.Random
import kotlin.reflect.KClass

/**
 * Test is in this project and not `settings` due to access to [DeveloperUiToolFactoryEp].
 */
class DeveloperToolsInstanceSettingsTest : IdeaTest() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @Test
  fun `Check various configurations persistent states`() {
    DeveloperToolsApplicationSettings.generalSettings.saveConfigurations.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveInputs.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveSensitiveInputs.set(true)

    val settings = object : DeveloperToolsInstanceSettings() {
      init {
        loadState(InstanceState())
      }
    }

    val developerUiTools = instantiateAllDeveloperUiTools(settings)

    // Delete all example values
    DeveloperToolsApplicationSettings.generalSettings.loadExamples.set(false)
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
    DeveloperToolsApplicationSettings.generalSettings.loadExamples.set(true)
    resetAllConfigurations(developerUiTools, settings, true)

    // Expect: No configurations have persisted because there are no property changes
    assertThat(settings.getState().developerToolsConfigurations).isEmpty()

    // Set the values to a random value
    setConfigurationPropertiesToRandomValues(developerUiTools, settings)

    // Expect: All configurations (with properties) have been persisted
    assertThat(settings.getState().developerToolsConfigurations)
      .hasSize(developerUiTools.filter {
        settings.getDeveloperToolConfigurations(it.developerToolFactoryEp.id)[0].properties.isNotEmpty()
      }.size)
  }

  @TestFactory
  fun `Test fromPersistent and toPersistent of built-in property types`(): List<DynamicNode> {
    val testVectors: Map<KClass<*>, Pair<Any, String>> = mapOf(
      Boolean::class to Pair(true, "true"),
      Int::class to Pair(42, "42"),
      Long::class to Pair(-84L, "-84"),
      Double::class to Pair(1234567.0, "1234567.0"),
      Float::class to Pair(1.2345f, "1.2345"),
      String::class to Pair("foo", "foo"),
      JBColor::class to Pair(JBColor.MAGENTA, "-65281"),
      LocaleContainer::class to Pair(LocaleContainer(Locale.forLanguageTag("de-DE")), "de-DE"),
      BigDecimal::class to Pair(BigDecimal(1.234), "1.2339999999999999857891452847979962825775146484375")
    )

    return builtInConfigurationPropertyTypes.map { (type, propertyType) ->
      dynamicTest(type.qualifiedName!!) {
        assertThat(testVectors).containsKey(type)
        val (inputValue, persistedValue) = testVectors[type]!!
        assertThat(propertyType.toPersistent(inputValue)).isEqualTo(persistedValue)
        assertThat(propertyType.fromPersistent(persistedValue)).isEqualTo(inputValue)
      }
    }
  }

  @Test
  fun `Legacy settings import test data for current plugin version exists`() {
    val legacyImportDirForPluginVersion = legacyImportDir.resolve(getPluginVersion())
    assertThat(legacyImportDirForPluginVersion).exists()
  }

  @Test
  @Disabled
  fun `Create legacy settings import test data for current plugin version`() {
    val legacyImportDirForPluginVersion = legacyImportDir.resolve(getPluginVersion())
    if (Files.notExists(legacyImportDirForPluginVersion)) {
      legacyImportDirForPluginVersion.createDirectories()
    }

    DeveloperToolsApplicationSettings.generalSettings.saveConfigurations.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveInputs.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveSensitiveInputs.set(true)

    val settings = object : DeveloperToolsInstanceSettings() {
      init {
        loadState(InstanceState())
      }
    }
    val developerUiTools = instantiateAllDeveloperUiTools(settings)
    setConfigurationPropertiesToRandomValues(developerUiTools, settings)

    legacyImportDirForPluginVersion.resolve(EXPECTED_CONFIGURATION_PROPERTIES_CSV_FILENAME).writer(options = arrayOf(CREATE, TRUNCATE_EXISTING)).use { writer ->
      CSVPrinter(writer, expectedConfigurationPropertiesCsvFormat.builder().setSkipHeaderRecord(false).build()).use { printer ->
        settings.getState().developerToolsConfigurations!!.forEach {
          it.properties!!.forEach { property ->
            val persistedValue = StatePropertyValueConverter().toString(property.value!!).split("|", limit = 2)
            printer.printRecord(it.developerToolId, property.key!!, property.type!!.name, persistedValue[0], persistedValue[1])
          }
        }
      }
    }

    legacyImportDirForPluginVersion.resolve(PERSISTED_STATE_XML_FILENAME).writer(options = arrayOf(CREATE, TRUNCATE_EXISTING)).use { writer ->
      val element = XmlSerializer.serialize(settings.getState())
      writer.write(JDOMUtil.write(element))
    }
  }

  @TestFactory
  fun `Test legacy settings import`(): List<DynamicNode> {
    val checkLegacyPersistedSettingsImport: (Path) -> DynamicContainer = { legacyImportVersionDir ->
      val persistedStateXmlFile = legacyImportVersionDir.resolve(PERSISTED_STATE_XML_FILENAME)
      val expectedConfigurationPropertiesCsvFile = legacyImportVersionDir.resolve(EXPECTED_CONFIGURATION_PROPERTIES_CSV_FILENAME)

      DeveloperToolsApplicationSettings.generalSettings.saveConfigurations.set(true)
      DeveloperToolsApplicationSettings.generalSettings.saveInputs.set(true)
      DeveloperToolsApplicationSettings.generalSettings.saveSensitiveInputs.set(true)

      val restoredSettings = object : DeveloperToolsInstanceSettings() {
        init {
          loadState(XmlSerializer.deserialize(persistedStateXmlFile.toUri().toURL(), InstanceState::class.java))
        }
      }

      val expectedConfigurationProperties = expectedConfigurationPropertiesCsvFile.bufferedReader(StandardCharsets.UTF_8).use { reader ->
        CSVParser(reader, expectedConfigurationPropertiesCsvFormat).records.map { record ->
          ExpectedConfigurationProperty(
            developerToolId = record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_DEVELOPER_TOOL_ID],
            propertyId = record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_ID],
            propertyValueType = record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE_TYPE_NAME],
            propertyValue = record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE],
            propertyType = record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_TYPE]
          )
        }
      }

      val checkProperty: (ExpectedConfigurationProperty) -> DynamicTest = { (developerToolId, propertyId, propertyValueType, propertyValue, propertyType) ->
        dynamicTest(propertyId) {
          val developerToolConfiguration = restoredSettings.getDeveloperToolConfigurations(developerToolId)[0]

          assertThat(developerToolConfiguration.persistentProperties).containsKey(propertyId)
          val property = developerToolConfiguration.persistentProperties[propertyId]

          assertThat(configurationPropertyTypesByNamesAndLegacyValueTypes).containsKey(propertyValueType)
          val developerToolConfigurationPropertyType = configurationPropertyTypesByNamesAndLegacyValueTypes[propertyValueType]

          val expectedValue = developerToolConfigurationPropertyType!!.fromPersistent(propertyValue)
          assertThat(property!!.value).isEqualTo(expectedValue)

          assertThat(property.type.name).isEqualTo(propertyType)
        }
      }

      dynamicContainer(
        legacyImportVersionDir.fileName.toString(),
        expectedConfigurationProperties.groupBy { it.developerToolId }.map {
          dynamicContainer(it.key, it.value.map(checkProperty))
        })
    }

    return Files.walk(legacyImportDir, 1)
      .filter { it != legacyImportDir }
      .filter { it.isDirectory() }
      .map(checkLegacyPersistedSettingsImport).toList()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun setConfigurationPropertiesToRandomValues(
    developerUiTools: List<DeveloperUiToolWrapper<*>>,
    settings: DeveloperToolsInstanceSettings
  ) {
    modifyAllConfigurationProperties(developerUiTools, settings) { property ->
      val randomValue = if (property.key == "timeZoneId") {
        val availableZoneIds = ZoneId.getAvailableZoneIds().toList()
        availableZoneIds[Random.Default.nextInt(0, availableZoneIds.size - 1)]
      }
      else {
        val defaultValue = property.defaultValue
        when (defaultValue) {
          is String -> defaultValue + "foo"
          is Int -> defaultValue + 1
          is Long -> defaultValue + 1
          is BigDecimal -> defaultValue.plus(BigDecimal.ONE)
          is Boolean -> !defaultValue
          is LocaleContainer -> {
            val availableLocales = Locale.getAvailableLocales()
            LocaleContainer(availableLocales[Random.Default.nextInt(0, availableLocales.size - 1)])
          }

          is Enum<*> -> {
            val enumConstants = defaultValue::class.java.enumConstants
            enumConstants[(defaultValue.ordinal + 1) % enumConstants.size]
          }

          is JBColor -> JBColor(
            Random.Default.nextInt(0, 255),
            Random.Default.nextInt(0, 255)
          )

          else -> throw IllegalStateException("Missing property type mapping for: " + defaultValue::class)
        }
      }
      property.reference.setWithUncheckedCast(randomValue, null)
    }
  }

  private fun resetAllConfigurations(
    developerUiTools: List<DeveloperUiToolWrapper<*>>,
    settings: DeveloperToolsInstanceSettings,
    loadExamples: Boolean
  ) {
    developerUiTools.forEach { (developerUiToolEp, _, _) ->
      settings.getDeveloperToolConfigurations(developerUiToolEp.id).forEach {
        val runnable = {
          it.reset(null, loadExamples)
        }
        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any())
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
        val runnable = {
          it.properties.values.forEach { property ->
            modify(property)
          }
        }
        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any())
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
        val runnable = {
          developerUiTool.createComponent()
          developerUiTool.activated()
        }
        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any())
        developerUiTools.add(
          DeveloperUiToolWrapper(
            developerToolFactoryEp = developerToolFactoryEp,
            developerUiTool = developerUiTool,
            developerToolConfiguration = developerToolConfiguration
          )
        )
      }
      else {
        Assertions.fail("No instance of tool was created: ${developerToolFactoryEp.id}")
      }
    }

    return developerUiTools
  }

  // -- Inner Type ---------------------------------------------------------- //

  private data class ExpectedConfigurationProperty(
    val developerToolId: String,
    val propertyId: String,
    val propertyValueType: String,
    val propertyValue: String,
    val propertyType: String
  )

  // -- Inner Type ---------------------------------------------------------- //

  private data class DeveloperUiToolWrapper<T : DeveloperUiTool>(
    val developerToolFactoryEp: DeveloperUiToolFactoryEp<out DeveloperUiToolFactory<*>>,
    val developerUiTool: T,
    val developerToolConfiguration: DeveloperToolConfiguration
  )

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val EXPECTED_CONFIGURATION_PROPERTIES_CSV_FILENAME = "expected-configuration-properties.csv"
    private const val PERSISTED_STATE_XML_FILENAME = "persisted-state.xml"
    private val legacyImportDir = Paths.get("src/test/resources/legacyImport")

    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_DEVELOPER_TOOL_ID = "developerToolId"
    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_ID = "propertyId"
    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_TYPE = "propertyType"
    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE_TYPE_NAME = "propertyValueTypeName"
    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE = "propertyValue"

    private val expectedConfigurationPropertiesCsvFormat = CSVFormat.Builder.create().setHeader(
      EXPECTED_CONFIGURATION_PROPERTIES_HEADER_DEVELOPER_TOOL_ID,
      EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_ID,
      EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_TYPE,
      EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE_TYPE_NAME,
      EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE
    ).setSkipHeaderRecord(true).build()

    @BeforeAll
    @JvmStatic
    fun beforeAll() {
      System.setProperty("java.awt.headless", "true")
    }

    @AfterAll
    @JvmStatic
    fun afterAll() {
      System.setProperty("java.awt.headless", "false")
    }
  }
}
