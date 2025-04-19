package dev.turingcomplete.intellijdevelopertoolsplugin.integrationtest

import com.intellij.openapi.util.JDOMUtil
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.XmlSerializer
import dev.turingcomplete.intellijdevelopertoolsplugin.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures.IdeaTest
import dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures.PluginUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.Companion.configurationPropertyTypesByNamesAndLegacyValueTypes
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.DeveloperToolConfigurationState
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.InstanceState
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.StatePropertyValueConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.testfixtures.DeveloperUiToolUnderTest
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.testfixtures.DeveloperUiToolsInstances.createDeveloperUiToolsUnderTest
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.Locale
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.writer
import kotlin.reflect.KClass
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class DeveloperToolsInstanceSettingsTest : IdeaTest() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @TestFactory
  fun `Check various configurations persistent states`(): List<DynamicNode> {
    val developerUiToolIdsWithoutPersistentState = setOf("rubber-duck", "intellij-internals")

    val testNodes = mutableListOf<DynamicNode>()

    DeveloperToolsApplicationSettings.generalSettings.saveConfigurations.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveInputs.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveSensitiveInputs.set(true)

    val settings = createDeveloperToolsInstanceSettings()

    val developerUiToolsUnderTest: List<DeveloperUiToolUnderTest<*>> =
      createDeveloperUiToolsUnderTest(fixture.project, disposable, settings)

    testNodes.add(
      dynamicTest("No persisted properties if values are empty") {
        // Delete all example values
        developerUiToolsUnderTest.forEach { it.resetConfiguration(loadExamples = false) }

        // Expect: No configurations have been persisted because there are no property changes
        // (The persistent state will only include configuration with modified properties)
        assertThat(settings.getState().developerToolsConfigurations).isEmpty()
      }
    )

    testNodes.add(
      dynamicTest("No persisted properties if they explicitly set to their example value") {
        // Set the values to the example value, despite `loadExamples` is disabled
        DeveloperToolsApplicationSettings.generalSettings.loadExamples.set(false)
        developerUiToolsUnderTest.forEach {
          it.modifyAllConfigurationProperties { property ->
            property.example?.let { exampleProvider -> exampleProvider() }
          }
        }

        // Expect: No configurations have persisted because there are no property changes
        // (Changes in the `loadExamples` setting are not reflected in the existing
        // configurations until the application was restarted. Therefore, during the
        // modification check, we will always check if the value is equal to the
        // example.)
        assertThat(settings.getState().developerToolsConfigurations).isEmpty()
      }
    )

    testNodes.add(
      dynamicTest("No persisted properties if they explicitly set to their default value") {
        // Set the values to the default value
        developerUiToolsUnderTest.forEach {
          it.modifyAllConfigurationProperties { property -> property.defaultValue }
        }

        // Expect: No configurations have persisted because there are no property changes
        assertThat(settings.getState().developerToolsConfigurations).isEmpty()
      }
    )

    testNodes.add(
      dynamicTest("No persisted properties after they have been reset") {
        // Load all example values
        DeveloperToolsApplicationSettings.Companion.generalSettings.loadExamples.set(true)
        developerUiToolsUnderTest.forEach { it.resetConfiguration(loadExamples = true) }

        // Expect: No configurations have persisted because there are no property changes
        assertThat(settings.getState().developerToolsConfigurations).isEmpty()
      }
    )

    // Set the values to a random value
    val randomValuesTestNodes =
      developerUiToolsUnderTest
        .filter { !developerUiToolIdsWithoutPersistentState.contains(it.id) }
        .map { developerUiToolUnderTest ->
          developerUiToolUnderTest.randomiseConfiguration()

          val actualPersistedStates: Map<String, DeveloperToolConfigurationState> =
            settings.getState().developerToolsConfigurations!!.associateBy { it.developerToolId!! }

          dynamicContainer(
            developerUiToolUnderTest.id,
            listOf(
              dynamicTest("Developer tool has a persisted state") {
                assertThat(actualPersistedStates).containsKey(developerUiToolUnderTest.id)
              },
              dynamicTest("All properties have an persisted state") {
                val actualDeveloperToolConfigurationPropertiesKeys =
                  actualPersistedStates[developerUiToolUnderTest.id]!!.properties!!.map { property
                    ->
                    property.key!!
                  }
                assertThat(actualDeveloperToolConfigurationPropertiesKeys)
                  .containsExactlyInAnyOrderElementsOf(
                    developerUiToolUnderTest.configuration.properties.keys
                  )
              },
            ),
          )
        }
    testNodes.add(dynamicContainer("Persisted state", randomValuesTestNodes))

    return testNodes
  }

  @TestFactory
  fun `Test fromPersistent and toPersistent of built-in property types`(): List<DynamicNode> {
    val testVectors: Map<KClass<*>, Pair<Any, String>> =
      mapOf(
        Boolean::class to Pair(true, "true"),
        Int::class to Pair(42, "42"),
        Long::class to Pair(-84L, "-84"),
        Double::class to Pair(1234567.0, "1234567.0"),
        Float::class to Pair(1.2345f, "1.2345"),
        String::class to Pair("foo", "foo"),
        JBColor::class to Pair(JBColor.MAGENTA, "-65281"),
        LocaleContainer::class to Pair(LocaleContainer(Locale.forLanguageTag("de-DE")), "de-DE"),
        BigDecimal::class to
          Pair(BigDecimal(1.234), "1.2339999999999999857891452847979962825775146484375"),
      )

    return DeveloperToolsInstanceSettings.builtInConfigurationPropertyTypes.map {
      (type, propertyType) ->
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
    val legacyImportDirForPluginVersion = legacyImportDir.resolve(PluginUtils.getPluginVersion())
    assertThat(legacyImportDirForPluginVersion).exists()
  }

  @Test
  @Disabled
  fun `Create legacy settings import test data for current plugin version`() {
    val legacyImportDirForPluginVersion = legacyImportDir.resolve(PluginUtils.getPluginVersion())
    if (Files.notExists(legacyImportDirForPluginVersion)) {
      legacyImportDirForPluginVersion.createDirectories()
    }

    DeveloperToolsApplicationSettings.generalSettings.saveConfigurations.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveInputs.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveSensitiveInputs.set(true)

    val settings = createDeveloperToolsInstanceSettings()

    val developerUiToolsUnderTest: List<DeveloperUiToolUnderTest<*>> =
      createDeveloperUiToolsUnderTest(fixture.project, disposable, settings)

    developerUiToolsUnderTest.forEach { it.randomiseConfiguration() }

    legacyImportDirForPluginVersion
      .resolve(EXPECTED_CONFIGURATION_PROPERTIES_CSV_FILENAME)
      .writer(options = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
      .use { writer ->
        CSVPrinter(
            writer,
            expectedConfigurationPropertiesCsvFormat.builder().setSkipHeaderRecord(false).build(),
          )
          .use { printer ->
            settings.getState().developerToolsConfigurations!!.forEach {
              it.properties!!.forEach { property ->
                val persistedValue =
                  StatePropertyValueConverter().toString(property.value!!).split("|", limit = 2)
                printer.printRecord(
                  it.developerToolId,
                  property.key!!,
                  property.type!!.name,
                  persistedValue[0],
                  persistedValue[1],
                )
              }
            }
          }
      }

    legacyImportDirForPluginVersion
      .resolve(PERSISTED_STATE_XML_FILENAME)
      .writer(options = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
      .use { writer ->
        val element = XmlSerializer.serialize(settings.getState())
        writer.write(JDOMUtil.write(element))
      }
  }

  @TestFactory
  fun `Test legacy settings import`(): List<DynamicNode> {
    val deletedProperties: List<(ExpectedConfigurationProperty) -> Boolean> =
      listOf({ it.developerToolId == "line-breaks-encoder-decoder" })

    val checkLegacyPersistedSettingsImport: (Path) -> DynamicContainer = { legacyImportVersionDir ->
      val persistedStateXmlFile = legacyImportVersionDir.resolve(PERSISTED_STATE_XML_FILENAME)
      val expectedConfigurationPropertiesCsvFile =
        legacyImportVersionDir.resolve(EXPECTED_CONFIGURATION_PROPERTIES_CSV_FILENAME)

      DeveloperToolsApplicationSettings.Companion.generalSettings.saveConfigurations.set(true)
      DeveloperToolsApplicationSettings.Companion.generalSettings.saveInputs.set(true)
      DeveloperToolsApplicationSettings.Companion.generalSettings.saveSensitiveInputs.set(true)

      val restoredSettings =
        createDeveloperToolsInstanceSettings(
          XmlSerializer.deserialize(
            persistedStateXmlFile.toUri().toURL(),
            InstanceState::class.java,
          )
        )

      val developerUiToolsUnderTest: Map<String, DeveloperUiToolUnderTest<*>> =
        createDeveloperUiToolsUnderTest(fixture.project, disposable, restoredSettings).associateBy {
          it.id
        }

      val expectedConfigurationProperties =
        expectedConfigurationPropertiesCsvFile.bufferedReader(StandardCharsets.UTF_8).use { reader
          ->
          CSVParser(reader, expectedConfigurationPropertiesCsvFormat).records.map { record ->
            ExpectedConfigurationProperty(
              developerToolId = record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_DEVELOPER_TOOL_ID],
              propertyKey = record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_ID],
              propertyValueType =
                record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE_TYPE_NAME],
              propertyValue =
                record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE].replace(
                  "\r\n",
                  System.lineSeparator(),
                ),
              propertyType = record[EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_TYPE],
            )
          }
        }

      val checkRestoredProperty: (ExpectedConfigurationProperty) -> DynamicTest =
        { (developerToolId, propertyId, propertyValueTypeName, propertyValue, propertyType) ->
          dynamicTest(propertyId) {
            val actualConfiguration = developerUiToolsUnderTest[developerToolId]!!.configuration

            // Property was loaded from the persisted state
            assertThat(actualConfiguration.persistentProperties).containsKey(propertyId)

            // Property was restored from persisted state during property registration
            val actualProperties =
              actualConfiguration.properties.values
                .flatMap { listOf(it.key, it.legacyKey).map { key -> key to it } }
                .toMap()
            assertThat(actualProperties).containsKey(propertyId)
            val actualProperty = actualProperties[propertyId]

            // Property type is allowed
            assertThat(configurationPropertyTypesByNamesAndLegacyValueTypes)
              .containsKey(propertyValueTypeName)

            val propertyValueType =
              configurationPropertyTypesByNamesAndLegacyValueTypes[propertyValueTypeName]

            val expectedValue = propertyValueType!!.fromPersistent(propertyValue)
            // Property value was correctly read from its persisted state
            assertThat(actualConfiguration.persistentProperties[propertyId]!!.value)
              .isEqualTo(expectedValue)
            // Property value was correctly restored from its persisted state
            // This test may in some cases not really test the restore, because the
            // value was overwritten during the tool initialization (e.g., source
            // to target text transformation).
            assertThat(actualProperty!!.reference.get()).isEqualTo(expectedValue)

            // Property type was correctly restored
            assertThat(actualProperty.type.name).isEqualTo(propertyType)
          }
        }

      dynamicContainer(
        legacyImportVersionDir.fileName.toString(),
        expectedConfigurationProperties
          .filter { expectedConfigurationProperty ->
            deletedProperties.none { it(expectedConfigurationProperty) }
          }
          .groupBy { it.developerToolId }
          .map { dynamicContainer(it.key, it.value.map(checkRestoredProperty)) },
      )
    }

    return Files.walk(legacyImportDir, 1)
      .filter { it != legacyImportDir }
      .filter { it.isDirectory() }
      .map(checkLegacyPersistedSettingsImport)
      .toList()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createDeveloperToolsInstanceSettings(
    instantState: InstanceState = InstanceState()
  ): DeveloperToolsInstanceSettings =
    object : DeveloperToolsInstanceSettings() {

      init {
        loadState(instantState)
      }
    }

  // -- Inner Type ---------------------------------------------------------- //

  private data class ExpectedConfigurationProperty(
    val developerToolId: String,
    val propertyKey: String,
    val propertyValueType: String,
    val propertyValue: String,
    val propertyType: String,
  )

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val EXPECTED_CONFIGURATION_PROPERTIES_CSV_FILENAME =
      "expected-configuration-properties.csv"
    private const val PERSISTED_STATE_XML_FILENAME = "persisted-state.xml"
    private val legacyImportDir =
      Paths.get(
        "src/test/resources/dev/turingcomplete/intellijdevelopertoolsplugin/integrationtest/legacyimport"
      )

    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_DEVELOPER_TOOL_ID = "developerToolId"
    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_ID = "propertyId"
    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_TYPE = "propertyType"
    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE_TYPE_NAME =
      "propertyValueTypeName"
    private const val EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE = "propertyValue"

    private val expectedConfigurationPropertiesCsvFormat =
      CSVFormat.Builder.create()
        .setHeader(
          EXPECTED_CONFIGURATION_PROPERTIES_HEADER_DEVELOPER_TOOL_ID,
          EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_ID,
          EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_TYPE,
          EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE_TYPE_NAME,
          EXPECTED_CONFIGURATION_PROPERTIES_HEADER_PROPERTY_VALUE,
        )
        .setSkipHeaderRecord(true)
        .build()
  }
}
