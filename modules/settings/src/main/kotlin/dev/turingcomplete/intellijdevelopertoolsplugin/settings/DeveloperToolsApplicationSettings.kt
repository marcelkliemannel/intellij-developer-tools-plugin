package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.ActionHandlingInstance.DIALOG
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.ActionHandlingInstance.TOOL_WINDOW
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Settings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsHandler.settingsContainer
import org.jdom.Element
import java.security.Provider
import java.security.Security

@Service
@State(
  name = "DeveloperToolsApplicationSettingsV1",
  storages = [Storage("developer-tools.xml")],
  category = SettingsCategory.TOOLS
)
class DeveloperToolsApplicationSettings : PersistentStateComponent<Element> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val generalSettings: GeneralSettings by lazy { SettingsHandler.create(GeneralSettings::class) }
  val internalSettings: InternalSettings by lazy { SettingsHandler.create(InternalSettings::class) }
  val jsonHandling: JsonHandlingSettings by lazy { SettingsHandler.create(JsonHandlingSettings::class) }

  private val allSettings = listOf<Settings>(generalSettings, internalSettings, jsonHandling)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    try {
      val bouncyCastleProviderClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
      val bouncyCastleProvider = bouncyCastleProviderClass.getConstructor().newInstance()
      Security.addProvider(bouncyCastleProvider as Provider)
    } catch (e: Exception) {
      log.debug("Can't load BouncyCastleProvider", e)
    }
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getState(): Element {
    val root = Element("Root")

    allSettings.forEach { settings ->
      val settingsContainer = settings.settingsContainer()
      val settingsElement = Element(settingsContainer.kclass.simpleName)

      settingsContainer.settingProperties
        .filter { it.value.isModified() }
        .forEach { settingName, settingProperty ->
          settingProperty.toPersistent()?.let {
            settingsElement.setAttribute(settingName, it)
          }
        }

      root.addContent(settingsElement)
    }

    return root
  }

  override fun loadState(state: Element) {
    applyLegacy(state)

    state.children.forEach { settingsElement ->
      val settings = allSettings
        .map { it.settingsContainer() }
        .firstOrNull { settingsContainer -> settingsContainer.kclass.simpleName == settingsElement.name }
      if (settings == null) {
        log.warn("Can't find settings class: ${settingsElement.name}")
        return@forEach
      }

      settings.settingProperties.forEach { (settingName, property) ->
        settingsElement.getAttributeValue(settingName)?.let {
          property.fromPersistent(it)
        }
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun applyLegacy(state: Element) {
    if (state.attributes.none { it.name != "name" }) {
      return
    }

    val generalSettingsElement = Element(GeneralSettings::class.simpleName)
    val internalSettingsElement = Element(InternalSettings::class.simpleName)

    state.attributes.iterator().run {
      while (hasNext()) {
        val attribute = next()
        val transformedValue: String? = when(attribute.name) {
          "selectedActionHandlingInstance" -> when(attribute.value) {
            "Tool Window" -> TOOL_WINDOW.name
            "Dialog" -> DIALOG.name
            else -> null
          }
          else -> attribute.value
        }
        if (transformedValue != null) {
          val targetSettingsElement = when(attribute.name) {
            "promoteAddOpenMainDialogActionToMainToolbar" -> internalSettingsElement
            else -> generalSettingsElement
          }
          targetSettingsElement.setAttribute(attribute.name, transformedValue)
        }
        remove()
      }
    }

    state.addContent(generalSettingsElement)
    state.addContent(internalSettingsElement)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val log = logger<DeveloperToolsInstanceSettings>()

    val instance: DeveloperToolsApplicationSettings
      get() = ApplicationManager.getApplication().getService(DeveloperToolsApplicationSettings::class.java)

    val generalSettings: GeneralSettings
      get() = instance.generalSettings

    val internalSettings: InternalSettings
      get() = instance.internalSettings

    val jsonHandling: JsonHandlingSettings
      get() = instance.jsonHandling
  }
}
