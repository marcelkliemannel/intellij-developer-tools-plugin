package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.openapi.util.JDOMUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.AnySettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Settings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsHandler.settingsContainer
import org.assertj.core.api.Assertions.assertThat
import org.jdom.Element
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

class DeveloperToolsApplicationSettingsTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @ParameterizedTest(name = "{1}")
  @MethodSource("settingsTestVectors")
  fun `test that the settings names have not been changed`(
    getSettings: (DeveloperToolsApplicationSettings) -> Settings,
    @Suppress("unused") xmlElementName: String,
    expectedSettingNames: Set<String>
  ) {
    val developerToolsApplicationSettings = DeveloperToolsApplicationSettings()

    assertThat(getSettings(developerToolsApplicationSettings).settingsContainer().settingProperties.keys)
      .containsExactlyInAnyOrderElementsOf(expectedSettingNames)
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("settingsTestVectors")
  fun `test restoring from XML`(
    getSettings: (DeveloperToolsApplicationSettings) -> Settings,
    xmlElementName: String,
    @Suppress("unused") expectedSettingNames: Set<String>
  ) {
    val developerToolsApplicationSettings = DeveloperToolsApplicationSettings()
    val settingsContainer = getSettings(developerToolsApplicationSettings).settingsContainer()

    val modifiedAttributes = settingsContainer.settingProperties.map { (settingName, property) ->
      settingName to property.get().modifyValue()
    }.toMap()

    val xmlAttributes = modifiedAttributes.map { (settingName, modifiedValue) ->
      val persistentValue = when (modifiedValue) {
        is Boolean -> modifiedValue.toString()
        is Int -> modifiedValue.toString()
        is Enum<*> -> modifiedValue.name
        else -> error("Unsupported type: ${this::class}")
      }
      "${settingName}=\"${persistentValue}\""
    }.joinToString(" ")
    val legacyState = """
        <component name="DeveloperToolsApplicationSettingsV1">
          <$xmlElementName $xmlAttributes />
        </component>
      """.parseXml()

    developerToolsApplicationSettings.loadState(legacyState)

    settingsContainer.settingProperties.forEach { (settingName, property) ->
      assertThat(property.isModified())
        .describedAs("$xmlElementName - $settingName")
        .isTrue

      assertThat(property.get())
        .describedAs("$xmlElementName - $settingName")
        .isEqualTo(modifiedAttributes[settingName])
    }
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("settingsTestVectors")
  fun `test persisting to XML`(
    getSettings: (DeveloperToolsApplicationSettings) -> Settings,
    xmlElementName: String,
    @Suppress("unused") expectedSettingNames: Set<String>
  ) {
    val developerToolsApplicationSettings = DeveloperToolsApplicationSettings()
    val settingsContainer = getSettings(developerToolsApplicationSettings).settingsContainer()

    val expectedAttributes: Map<String, String> = settingsContainer.settingProperties.map { (settingName, property) ->
      val modifiedValue = property.get().modifyValue()
      property.set(modifiedValue)
      val expectedPersistentValue = when (modifiedValue) {
        is Boolean -> modifiedValue.toString()
        is Int -> modifiedValue.toString()
        is Enum<*> -> modifiedValue.name
        else -> error("Unsupported type: ${this::class}")
      }
      settingName to expectedPersistentValue
    }.toMap()

    val actualState: Element = developerToolsApplicationSettings.state
    val settingsElement: Element = actualState.content.find { (it as Element).name == xmlElementName } as Element
    assertThat(settingsElement).isNotNull

    assertThat(settingsElement.attributes.associate { it.name to it.value })
      .containsAllEntriesOf(expectedAttributes)
  }

  @Test
  fun `test loading legacy state`() {
    val developerToolsApplicationSettings = DeveloperToolsApplicationSettings()

    val legacyAttributeNames = listOf(
      "addOpenMainDialogActionToMainToolbar",
      "autoDetectActionHandlingInstance",
      "editorShowSpecialCharacters",
      "editorShowWhitespaces",
      "editorSoftWraps",
      "hideWorkbenchTabsOnSingleTab",
      "loadExamples",
      "promoteAddOpenMainDialogActionToMainToolbar",
      "saveConfigurations",
      "saveInputs",
      "saveSensitiveInputs",
      "selectedActionHandlingInstance",
      "showInternalTools",
      "toolsMenuTreeOrderAlphabetically",
      "toolsMenuTreeShowGroupNodes"
    )

    val getSettings: (String) -> Settings = { legacyAttributeName ->
      when (legacyAttributeName) {
        "promoteAddOpenMainDialogActionToMainToolbar" -> developerToolsApplicationSettings.internalSettings
        else -> developerToolsApplicationSettings.generalSettings
      }
    }

    val modifiedLegacyAttributes: Map<String, Any> = legacyAttributeNames.associate { legacyAttributeName ->
      val initialValue = getSettings(legacyAttributeName).getSetting<Any, Annotation, AnySettingProperty>(legacyAttributeName).get()
      legacyAttributeName to initialValue.modifyValue()
    }

    val legacyState = """
      <component 
        name="DeveloperToolsApplicationSettingsV1"
        ${modifiedLegacyAttributes.asSequence().joinToString(" ") { "${it.key}=\"${it.value}\"" }}
      />
    """.parseXml()
    developerToolsApplicationSettings.loadState(legacyState)

    legacyAttributeNames.forEach { legacyAttributeName ->
      val settingProperty = getSettings(legacyAttributeName).getSetting<Any, Annotation, AnySettingProperty>(legacyAttributeName)
      assertThat(settingProperty.isModified()).describedAs(legacyAttributeName).isTrue
      assertThat(settingProperty.get()).describedAs(legacyAttributeName).isEqualTo(modifiedLegacyAttributes[legacyAttributeName])
    }
  }

  private fun Any.modifyValue(): Any = when (this) {
    is Boolean -> !this
    is Int -> this + 1
    is Enum<*> -> {
      val enumConstants = this::class.java.enumConstants
      enumConstants[(this.ordinal + 1) % enumConstants.size]
    }

    else -> error("Unsupported type: ${this::class}")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun String.parseXml(): Element = JDOMUtil.load(this)

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  
  companion object {

    @JvmStatic
    fun settingsTestVectors() = listOf(
      arguments(
        { it: DeveloperToolsApplicationSettings -> it.generalSettings },
        "GeneralSettings",
        setOf(
          "addOpenMainDialogActionToMainToolbar",
          "loadExamples",
          "saveConfigurations",
          "saveInputs",
          "saveSensitiveInputs",
          "editorSoftWraps",
          "editorShowSpecialCharacters",
          "editorShowWhitespaces",
          "toolsMenuTreeShowGroupNodes",
          "toolsMenuTreeOrderAlphabetically",
          "autoDetectActionHandlingInstance",
          "selectedActionHandlingInstance",
          "showInternalTools",
          "hideWorkbenchTabsOnSingleTab",
          "dialogIsModal"
        )
      ),
      arguments(
        { it: DeveloperToolsApplicationSettings -> it.internalSettings },
        "InternalSettings",
        setOf(
          "promoteAddOpenMainDialogActionToMainToolbar"
        )
      )
    )
  }
}
