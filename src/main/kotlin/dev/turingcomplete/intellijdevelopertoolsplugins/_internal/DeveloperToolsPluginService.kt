package dev.turingcomplete.intellijdevelopertoolsplugins._internal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.XCollection.Style.v2
import com.jetbrains.rd.util.getOrCreate
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import java.util.concurrent.ConcurrentHashMap

@State(name = "DeveloperToolsPluginService",
       storages = [Storage("developer-tools.xml")])
internal class DeveloperToolsPluginService : PersistentStateComponent<DeveloperToolsPluginService.State?> {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val instance: DeveloperToolsPluginService
      get() = ApplicationManager.getApplication().getService(DeveloperToolsPluginService::class.java)
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val developerToolsConfigurations = ConcurrentHashMap<String, DeveloperToolConfiguration>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun getOrCreateDeveloperToolConfiguration(id: String) =
    developerToolsConfigurations.getOrCreate(id) { DeveloperToolConfiguration() }

  override fun getState(): State {
    val developerToolsConfigurationProperties: List<DeveloperToolConfigurationProperty> =
      developerToolsConfigurations.asSequence().flatMap { (developerToolId, developerToolConfiguration) ->
        developerToolConfiguration.properties.map {
          DeveloperToolConfigurationProperty(developerToolId = developerToolId, key = it.key, value = it.value)
        }
      }.toList()
    return State(developerToolsConfigurationProperties)
  }

  override fun loadState(state: State) {
    state.developerToolsConfigurationProperties?.groupBy { it.developerToolId }?.forEach { (developerToolId, properties) ->
      val developerToolConfiguration = DeveloperToolConfiguration().apply {
        this.properties.putAll(properties.filter { it.key != null && it.value != null }.associate { it.key!! to it.value!! })
      }
      developerToolsConfigurations[developerToolId!!] = developerToolConfiguration
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class State(
          @get:XCollection(style = v2, elementName = "developerToolsConfigurationProperties", elementTypes = [DeveloperToolConfigurationProperty::class])
          var developerToolsConfigurationProperties: List<DeveloperToolConfigurationProperty>? = null
  )

  @Tag(value = "property")
  data class DeveloperToolConfigurationProperty(
          @get:Attribute("developerToolsId")
          var developerToolId: String? = null,
          @get:Attribute("key")
          var key: String? = null,
          @get:Attribute("value")
          var value: Any? = null
  )
}
