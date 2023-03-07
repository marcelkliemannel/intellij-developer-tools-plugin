package dev.turingcomplete.intellijdevelopertoolsplugins._internal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.XCollection.Style.v2
import com.jetbrains.rd.util.getOrCreate
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import io.ktor.util.reflect.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.cast

@State(name = "DeveloperToolsPluginService",
       storages = [Storage("developer-tools.xml")])
internal class DeveloperToolsPluginService : PersistentStateComponent<DeveloperToolsPluginService.State?> {
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

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @Tag(value = "property")
  data class DeveloperToolConfigurationProperty(
          @get:Attribute("developerToolsId")
          var developerToolId: String? = null,
          @get:Attribute("key")
          var key: String? = null,
          @get:Attribute("value", converter = DeveloperToolConfigurationValueConverter::class)
          var value: Any? = null
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class DeveloperToolConfigurationValueConverter : Converter<Any>() {

    override fun toString(value: Any): String {
      val serializedValue = when (value) {
        is Enum<*> -> value.name
        is Boolean, is Int, is Long, is Float, is Double -> value.toString()
        is String -> value
        else -> error("Unsupported configuration property: ${value::class.qualifiedName}")
      }
      return "${value::class.qualifiedName}${PROPERTY_TYPE_VALUE_DELIMITER}$serializedValue"
    }

    /**
     * If the value can't be restored this method must return null. All
     * properties with null values will be filtered in
     * [DeveloperToolsPluginService.loadState].
     */
    override fun fromString(serializedValue: String): Any? {
      val valueAndType = serializedValue.split(PROPERTY_TYPE_VALUE_DELIMITER, limit = 2)
      check(valueAndType.size == 2) { "Malformed serialized value: $serializedValue" }
      val valueType = valueAndType[0]
      val value = valueAndType[1]
      return when {
        valueType == Boolean::class.qualifiedName -> value.toBoolean()
        valueType == Int::class.qualifiedName -> value.toInt()
        valueType == Long::class.qualifiedName -> value.toLong()
        valueType == Float::class.qualifiedName -> value.toFloat()
        valueType == Double::class.qualifiedName -> value.toDouble()
        valueType == String::class.qualifiedName -> value
        Class.forName(valueType).isEnum -> Class.forName(valueType).enumConstants.first { Enum::class.cast(it).name == value }
        else -> error("Unsupported configuration property: $valueType")
      }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val instance: DeveloperToolsPluginService
      get() = ApplicationManager.getApplication().getService(DeveloperToolsPluginService::class.java)

    private const val PROPERTY_TYPE_VALUE_DELIMITER = "|"

    private val SUPPORTED_TYPES = setOf<KClass<*>>(Boolean::class, Int::class, Long::class, Float::class, Double::class, String::class)

    fun checkConfigurationPropertyType(type: KClass<*>) {
      check(type.java.isEnum || SUPPORTED_TYPES.contains(type)) {
        "Unsupported configuration property type: ${type.qualifiedName}"
      }
    }
  }
}
