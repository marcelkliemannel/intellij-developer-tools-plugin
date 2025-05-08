package dev.turingcomplete.intellijdevelopertoolsplugin.integrationtest

import com.intellij.openapi.util.JDOMUtil
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.XmlSerializer
import dev.turingcomplete.intellijdevelopertoolsplugin.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginInfo
import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginInfo.PluginVersion.Companion.toPluginVersion
import dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures.IdeaTest
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.Companion.configurationPropertyTypesByNamesAndLegacyValueTypes
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.DeveloperToolConfigurationState
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.InstanceState
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.StatePropertyValueConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettingsLegacy
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.testfixtures.DeveloperUiToolUnderTest
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.testfixtures.DeveloperUiToolsInstances.createDeveloperUiToolsUnderTest
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.Locale
import java.util.SortedMap
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.io.path.writer
import kotlin.reflect.KClass
import kotlin.streams.asSequence
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
    val legacyImportDirForPluginVersion =
      instanceSettingsResourcesDir.resolve(PluginInfo.pluginVersion.toString())
    assertThat(legacyImportDirForPluginVersion).exists()
  }

  @Test
  @Disabled
  fun `Create settings import test data for current plugin version`() {
    val legacyImportDirForPluginVersion =
      instanceSettingsResourcesDir.resolve(PluginInfo.pluginVersion.toString())
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
            settings
              .getState()
              .developerToolsConfigurations!!
              .sortedBy { it.developerToolId }
              .forEach {
                it.properties!!
                  .sortedBy { it.key }
                  .forEach { property ->
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
      .resolve(INSTANCE_SETTINGS_PERSISTED_STATE_XML_FILENAME)
      .writer(options = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
      .use { writer ->
        val element = XmlSerializer.serialize(settings.getState())
        writer.write(JDOMUtil.write(element))
      }
  }

  @ParameterizedTest
  @ValueSource(
    strings =
      [
        EXPECTED_CONFIGURATION_PROPERTIES_CSV_FILENAME,
        RENAMED_CONFIGURATION_PROPERTIES_CSV_FILENAME,
        REMOVED_CONFIGURATION_PROPERTIES_CSV_FILENAME,
      ]
  )
  fun `Changed configuration properties test data for current plugin version exists`(
    fileName: String
  ) {
    val changedConfigurationPropertiesFile =
      instanceSettingsResourcesDir.resolve(PluginInfo.pluginVersion.toString()).resolve(fileName)
    assertThat(changedConfigurationPropertiesFile).exists()
  }

  @Test
  @Disabled
  fun `Collect changed configuration properties of current plugin version`() {
    val pluginVersion =
      walkInstanceSettingsResourcesDir().map { it.name.toPluginVersion() }.sorted().toList()
    val previousPluginVersion = pluginVersion[pluginVersion.lastIndex - 1]
    assertThat(previousPluginVersion).isLessThan(PluginInfo.pluginVersion)

    val instanceSettingsVersionDir =
      walkInstanceSettingsResourcesDir()
        .filter { it.name == previousPluginVersion.toString() }
        .first()

    val persistedStateXmlFile =
      instanceSettingsVersionDir.resolve(INSTANCE_SETTINGS_PERSISTED_STATE_XML_FILENAME)
    val expectedConfigurationPropertiesCsvFile =
      instanceSettingsVersionDir.resolve(EXPECTED_CONFIGURATION_PROPERTIES_CSV_FILENAME)

    DeveloperToolsApplicationSettings.generalSettings.saveConfigurations.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveInputs.set(true)
    DeveloperToolsApplicationSettings.generalSettings.saveSensitiveInputs.set(true)

    val restoredSettings =
      createDeveloperToolsInstanceSettings(
        instantState =
          XmlSerializer.deserialize(
            persistedStateXmlFile.toUri().toURL(),
            InstanceState::class.java,
          )
      )

    val developerUiToolsUnderTest: Map<String, DeveloperUiToolUnderTest<*>> =
      createDeveloperUiToolsUnderTest(fixture.project, disposable, restoredSettings).associateBy {
        it.id
      }

    data class RenamedConfigurationProperty(
      val developerToolId: String,
      val propertyKey: String,
      val propertyKeyAfterLegacies: String,
    )
    data class RemovedConfigurationProperty(val developerToolId: String, val propertyKey: String)

    val renamedConfigurationProperties = mutableListOf<RenamedConfigurationProperty>()
    val removedConfigurationProperties = mutableListOf<RemovedConfigurationProperty>()

    readExpectedConfigurationProperties(expectedConfigurationPropertiesCsvFile).forEach {
      (developerToolId, propertyKey, _, _, _) ->
      // If `actualConfiguration` is null, the whole developer tool was removed
      val actualConfiguration = developerUiToolsUnderTest[developerToolId]?.configuration

      if (actualConfiguration?.properties?.containsKey(propertyKey) == true) {
        return@forEach
      }

      val propertyKeyAfterLegacies =
        DeveloperToolsInstanceSettingsLegacy.applyConfigurationPropertyKeyLegacies(
          null,
          developerToolId,
          propertyKey,
        )

      if (
        propertyKeyAfterLegacies != propertyKey &&
          actualConfiguration != null &&
          actualConfiguration.properties.containsKey(propertyKeyAfterLegacies)
      ) {
        renamedConfigurationProperties.add(
          RenamedConfigurationProperty(
            developerToolId = developerToolId,
            propertyKey = propertyKey,
            propertyKeyAfterLegacies = propertyKeyAfterLegacies,
          )
        )
      } else {
        removedConfigurationProperties.add(
          RemovedConfigurationProperty(developerToolId = developerToolId, propertyKey = propertyKey)
        )
      }
    }

    fun writePropertiesFile(
      filename: String,
      csvFormat: CSVFormat,
      writeProperties: CSVPrinter.() -> Unit,
    ) {
      instanceSettingsResourcesDir
        .resolve(PluginInfo.pluginVersion.toString())
        .resolve(filename)
        .apply { writeText("") }
        .writer(options = arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
        .use { writer ->
          val csvPrinter =
            CSVPrinter(writer, csvFormat.builder().setSkipHeaderRecord(false).build())
          writeProperties(csvPrinter)
        }
    }

    writePropertiesFile(
      RENAMED_CONFIGURATION_PROPERTIES_CSV_FILENAME,
      renamedConfigurationPropertiesCsvFormat,
    ) {
      renamedConfigurationProperties.forEach {
        printRecord(it.developerToolId, it.propertyKey, it.propertyKeyAfterLegacies)
      }
    }

    writePropertiesFile(
      REMOVED_CONFIGURATION_PROPERTIES_CSV_FILENAME,
      removedConfigurationPropertiesCsvFormat,
    ) {
      removedConfigurationProperties.forEach { printRecord(it.developerToolId, it.propertyKey) }
    }
  }

  @TestFactory
  fun `Test legacy settings import`(): List<DynamicNode> {
    val renamedProperties = collectRenamedConfigurationProperties()
    val removedProperties = collectRemovedConfigurationProperties()

    val checkInstanceSettings: (Path) -> DynamicContainer = { instanceSettingsVersionDir ->
      val persistedStateXmlFile =
        instanceSettingsVersionDir.resolve(INSTANCE_SETTINGS_PERSISTED_STATE_XML_FILENAME)
      val expectedConfigurationPropertiesCsvFile =
        instanceSettingsVersionDir.resolve(EXPECTED_CONFIGURATION_PROPERTIES_CSV_FILENAME)

      DeveloperToolsApplicationSettings.generalSettings.saveConfigurations.set(true)
      DeveloperToolsApplicationSettings.generalSettings.saveInputs.set(true)
      DeveloperToolsApplicationSettings.generalSettings.saveSensitiveInputs.set(true)

      val restoredSettings =
        createDeveloperToolsInstanceSettings(
          instantState =
            XmlSerializer.deserialize(
              persistedStateXmlFile.toUri().toURL(),
              InstanceState::class.java,
            )
        )

      val developerUiToolsUnderTest: Map<String, DeveloperUiToolUnderTest<*>> =
        createDeveloperUiToolsUnderTest(fixture.project, disposable, restoredSettings).associateBy {
          it.id
        }

      val expectedProperties =
        readExpectedConfigurationProperties(expectedConfigurationPropertiesCsvFile)

      val checkRestoredProperty: (ExpectedConfigurationProperty) -> DynamicTest =
        {
          (
            developerToolId,
            expectedPropertyKey,
            expectedPropertyValueTypeName,
            expectedPropertyValue,
            expectedPropertyType) ->
          dynamicTest(expectedPropertyKey) {
            val actualConfiguration = developerUiToolsUnderTest[developerToolId]?.configuration

            assertThat(actualConfiguration).isNotNull

            // Property was loaded from the persisted state
            val expectedPropertyKeyAfterRename =
              applyRenamedConfigurationPropertyKeys(
                renamedConfigurationProperties = renamedProperties,
                developerToolId = developerToolId,
                propertyKey = expectedPropertyKey,
              )
            assertThat(actualConfiguration!!.persistentProperties)
              .containsKey(expectedPropertyKeyAfterRename)

            // Property was restored from persisted state during property registration
            assertThat(actualConfiguration.properties).containsKey(expectedPropertyKeyAfterRename)
            val actualProperty = actualConfiguration.properties[expectedPropertyKeyAfterRename]

            // Property type is allowed
            assertThat(configurationPropertyTypesByNamesAndLegacyValueTypes)
              .containsKey(expectedPropertyValueTypeName)

            val propertyValueType =
              configurationPropertyTypesByNamesAndLegacyValueTypes[expectedPropertyValueTypeName]

            val expectedValue = propertyValueType!!.fromPersistent(expectedPropertyValue)
            // Property value was correctly read from its persisted state
            assertThat(
                actualConfiguration.persistentProperties[expectedPropertyKeyAfterRename]!!.value
              )
              .isEqualTo(expectedValue)
            // Property value was correctly restored from its persisted state
            // This test may in some cases not really test the restore, because the
            // value was overwritten during the tool initialization (e.g., source
            // to target text transformation).
            assertThat(actualProperty!!.reference.get()).isEqualTo(expectedValue)

            // Property type was correctly restored
            assertThat(actualProperty.type.name).isEqualTo(expectedPropertyType)
          }
        }

      dynamicContainer(
        instanceSettingsVersionDir.fileName.toString(),
        expectedProperties
          .filter { expectedProperty ->
            removedProperties.values.none {
              it[expectedProperty.developerToolId]?.contains(expectedProperty.propertyKey) == true
            }
          }
          .groupBy { it.developerToolId }
          .map { expectedProperty ->
            dynamicContainer(
              expectedProperty.key,
              expectedProperty.value.map(checkRestoredProperty),
            )
          },
      )
    }

    return walkInstanceSettingsResourcesDir().map(checkInstanceSettings).toList()
  }

  private fun readExpectedConfigurationProperties(
    expectedConfigurationPropertiesCsvFile: Path
  ): List<ExpectedConfigurationProperty> =
    expectedConfigurationPropertiesCsvFile.bufferedReader().use { reader ->
      CSVParser(reader, expectedConfigurationPropertiesCsvFormat).records.map { record ->
        val developerToolId = record[CSV_HEADER_DEVELOPER_TOOL_ID]
        ExpectedConfigurationProperty(
          developerToolId = developerToolId,
          propertyKey = record[CSV_HEADER_PROPERTY_KEY],
          propertyValueType = record[CSV_HEADER_PROPERTY_VALUE_TYPE_NAME],
          propertyValue = record[CSV_HEADER_PROPERTY_VALUE].replace("\r\n", System.lineSeparator()),
          propertyType = record[CSV_HEADER_PROPERTY_TYPE],
        )
      }
    }

  private fun applyRenamedConfigurationPropertyKeys(
    renamedConfigurationProperties:
      SortedMap<PluginInfo.PluginVersion, Map<String, Map<String, String>>>,
    developerToolId: String,
    propertyKey: String,
  ): String {
    var newPropertyName = propertyKey
    for ((_, renamedProperties) in renamedConfigurationProperties) {
      if (renamedProperties.containsKey(developerToolId)) {
        val renamedPropertiesForDeveloperTool = renamedProperties[developerToolId]
        if (renamedPropertiesForDeveloperTool?.containsKey(propertyKey) == true) {
          newPropertyName = renamedPropertiesForDeveloperTool[propertyKey]!!
        }
      }
    }
    return newPropertyName
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

  private fun collectRenamedConfigurationProperties():
    SortedMap<PluginInfo.PluginVersion, Map<String, Map<String, String>>> =
    walkInstanceSettingsResourcesDir()
      .mapNotNull { dir ->
        val csvFile = dir.resolve(RENAMED_CONFIGURATION_PROPERTIES_CSV_FILENAME)
        if (!csvFile.exists()) return@mapNotNull null

        val properties =
          csvFile.bufferedReader().use { reader ->
            CSVParser(reader, renamedConfigurationPropertiesCsvFormat).records.map {
              Triple(
                it.get(CSV_HEADER_DEVELOPER_TOOL_ID),
                it.get(CSV_HEADER_OLD_PROPERTY_KEY),
                it.get(CSV_HEADER_NEW_PROPERTY_KEY),
              )
            }
          }

        dir.name.toPluginVersion() to properties
      }
      .associate { (pluginVersion, props) ->
        pluginVersion to
          props
            .groupBy { it.first }
            .mapValues { (_, group) -> group.associate { it.second to it.third } }
      }
      .toSortedMap()

  private fun collectRemovedConfigurationProperties():
    SortedMap<PluginInfo.PluginVersion, Map<String, List<String>>> =
    Files.walk(instanceSettingsResourcesDir, 1)
      .asSequence()
      .filter { it != instanceSettingsResourcesDir && it.isDirectory() }
      .mapNotNull { dir ->
        val csvFile = dir.resolve(REMOVED_CONFIGURATION_PROPERTIES_CSV_FILENAME)
        if (!csvFile.exists()) return@mapNotNull null

        val properties =
          csvFile.bufferedReader().use { reader ->
            CSVParser(reader, removedConfigurationPropertiesCsvFormat).records.groupBy({
              it.get(CSV_HEADER_DEVELOPER_TOOL_ID)
            }) {
              it.get(CSV_HEADER_PROPERTY_KEY)
            }
          }

        dir.name.toPluginVersion() to properties
      }
      .toMap()
      .toSortedMap()

  private fun walkInstanceSettingsResourcesDir(): Sequence<Path> =
    Files.walk(instanceSettingsResourcesDir, 1)
      .filter { it != instanceSettingsResourcesDir }
      .filter { it.isDirectory() }
      .asSequence()

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
    private const val RENAMED_CONFIGURATION_PROPERTIES_CSV_FILENAME =
      "renamed-configuration-properties.csv"
    private const val REMOVED_CONFIGURATION_PROPERTIES_CSV_FILENAME =
      "removed-configuration-properties.csv"
    private const val INSTANCE_SETTINGS_PERSISTED_STATE_XML_FILENAME =
      "instance-settings-persisted-state.xml"
    private val instanceSettingsResourcesDir =
      Paths.get(
        "src/test/resources/dev/turingcomplete/intellijdevelopertoolsplugin/integrationtest/instancesettings"
      )

    private const val CSV_HEADER_DEVELOPER_TOOL_ID = "developerToolId"
    private const val CSV_HEADER_PROPERTY_KEY = "propertyKey"
    private const val CSV_HEADER_OLD_PROPERTY_KEY = "oldPropertyKey"
    private const val CSV_HEADER_NEW_PROPERTY_KEY = "newPropertyKey"
    private const val CSV_HEADER_PROPERTY_TYPE = "propertyType"
    private const val CSV_HEADER_PROPERTY_VALUE_TYPE_NAME = "propertyValueTypeName"
    private const val CSV_HEADER_PROPERTY_VALUE = "propertyValue"

    private val expectedConfigurationPropertiesCsvFormat =
      CSVFormat.Builder.create()
        .setHeader(
          CSV_HEADER_DEVELOPER_TOOL_ID,
          CSV_HEADER_PROPERTY_KEY,
          CSV_HEADER_PROPERTY_TYPE,
          CSV_HEADER_PROPERTY_VALUE_TYPE_NAME,
          CSV_HEADER_PROPERTY_VALUE,
        )
        .setSkipHeaderRecord(true)
        .build()

    private val renamedConfigurationPropertiesCsvFormat =
      CSVFormat.Builder.create()
        .setHeader(
          CSV_HEADER_DEVELOPER_TOOL_ID,
          CSV_HEADER_OLD_PROPERTY_KEY,
          CSV_HEADER_NEW_PROPERTY_KEY,
        )
        .setSkipHeaderRecord(true)
        .build()

    private val removedConfigurationPropertiesCsvFormat =
      CSVFormat.Builder.create()
        .setHeader(CSV_HEADER_DEVELOPER_TOOL_ID, CSV_HEADER_PROPERTY_KEY)
        .setSkipHeaderRecord(true)
        .build()
  }
}
