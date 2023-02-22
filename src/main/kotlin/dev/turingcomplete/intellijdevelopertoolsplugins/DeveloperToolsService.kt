package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import kotlin.reflect.KClass

@State(name = "DeveloperToolsService",
       storages = [Storage("DeveloperTools.xml")])
class DeveloperToolsService : PersistentStateComponent<DeveloperToolsService> {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val instance: DeveloperToolsService
      get() = ApplicationManager.getApplication().getService(DeveloperToolsService::class.java)
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val developerToolConfiguration = mutableMapOf<KClass<out DeveloperTool>, MutableMap<String, Any>>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun unsetProperty(developerTool: KClass<out DeveloperTool>, key: String) {
    developerToolConfiguration[developerTool]?.let { configuration ->
      configuration.remove(key)
      if (configuration.isEmpty()) {
        developerToolConfiguration.remove(developerTool)
      }
    }
  }

  fun setProperty(developerTool: KClass<out DeveloperTool>, key: String, value: Any) {
    developerToolConfiguration.getOrPut(developerTool) { mutableMapOf() }[key] = value
  }

  fun <T> getProperty(developerTool: KClass<out DeveloperTool>, key: String, defaultValue: T) : T {
    @Suppress("UNCHECKED_CAST")
    return developerToolConfiguration.get(developerTool)?.get(key) as T ?: defaultValue
  }

  override fun getState(): DeveloperToolsService {
    return this
  }

  override fun loadState(state: DeveloperToolsService) {
    XmlSerializerUtil.copyBean(state, this)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
