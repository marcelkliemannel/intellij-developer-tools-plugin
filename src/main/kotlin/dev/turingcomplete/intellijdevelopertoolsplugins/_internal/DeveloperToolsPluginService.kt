package dev.turingcomplete.intellijdevelopertoolsplugins._internal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.ui.JBColor
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

@State(
  name = "DeveloperToolsPluginService",
  storages = [Storage("developer-tools.xml")]
)
internal class DeveloperToolsPluginService : PersistentStateComponent<DeveloperToolsPluginService.State?> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val developerToolsConfigurations = ConcurrentHashMap<String, DeveloperToolConfiguration>()
  private val lastSelectedContentNodeId: ObservableMutableProperty<String?> = AtomicProperty(null)
  private val loadExamples: ObservableMutableProperty<Boolean> = AtomicProperty(LOAD_EXAMPLES_DEFAULT)
  private val saveInputs: ObservableMutableProperty<Boolean> = AtomicProperty(SAVE_INPUTS_DEFAULT)
  private val inputs = ConcurrentHashMap<String, String?>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun getOrCreateDeveloperToolConfiguration(id: String) =
    developerToolsConfigurations.getOrCreate(id) { DeveloperToolConfiguration() }

  override fun getState(): State {
    val stateDeveloperToolsConfigurations = developerToolsConfigurations.asSequence()
      .flatMap { (developerToolId, developerToolConfiguration) ->
        developerToolConfiguration.properties.map {
          Property(referenceId = developerToolId, key = it.key, value = it.value)
        }
      }.toList()
    loadExamples.afterChange {  }
    return State(
      developerToolsConfigurations = stateDeveloperToolsConfigurations,
      lastSelectedContentNodeId = lastSelectedContentNodeId.get(),
      loadExamples = loadExamples.get(),
      saveInputs = saveInputs.get()
    )
  }

  override fun loadState(state: State) {
    developerToolsConfigurations.clear()
    state.developerToolsConfigurations?.filter { it.referenceId != null && it.key != null && it.value != null }
      ?.groupBy { it.referenceId!! }
      ?.forEach { (developerToolId, properties) ->
        val developerToolConfiguration = DeveloperToolConfiguration().apply {
          this.properties.putAll(
            properties.filter { it.key != null && it.value != null }.associate { it.key!! to it.value!! }
          )
        }
        developerToolsConfigurations[developerToolId] = developerToolConfiguration
      }

    lastSelectedContentNodeId.set(state.lastSelectedContentNodeId)
    loadExamples.set(state.loadExamples ?: LOAD_EXAMPLES_DEFAULT)
    saveInputs.set(state.saveInputs ?: SAVE_INPUTS_DEFAULT)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class State(
    @get:XCollection(style = v2, elementName = "developerToolsConfigurations")
    var developerToolsConfigurations: List<Property>? = null,
    @get:Attribute("lastSelectedContentNodeId")
    var lastSelectedContentNodeId: String? = null,
    @get:Attribute("loadExamples")
    var loadExamples: Boolean? = null,
    @get:Attribute("saveInputs")
    var saveInputs: Boolean? = null,
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @Tag(value = "property")
  data class Property(
    @get:Attribute("referenceId")
    var referenceId: String? = null,
    @get:Attribute("key")
    var key: String? = null,
    @get:Attribute("value", converter = StatePropertyValueConverter::class)
    var value: Any? = null
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class StatePropertyValueConverter : Converter<Any>() {

    override fun toString(value: Any): String {
      val (serializedValue, valueType) = when (value) {
        is Enum<*> -> {
          // It's important to use the Java qualified name here because it
          // separates subclass names with a `$` and not with a `.` as in
          // Kotlin. Otherwise, the deserialization via `Class.forName()` would
          // not work.
          Pair(value.name, value::class.java.name)
        }

        is Boolean, is Int, is Long, is Float, is Double -> value.toString() to value::class.qualifiedName!!
        is String -> value to String::class.qualifiedName!!
        is JBColor -> value.rgb.toString() to JBColor::class.qualifiedName!!
        else -> error("Unsupported configuration property type: ${value::class.qualifiedName}")
      }
      return "${valueType}${PROPERTY_TYPE_VALUE_DELIMITER}$serializedValue"
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
      return when (valueType) {
        Boolean::class.qualifiedName -> value.toBoolean()
        Int::class.qualifiedName -> value.toInt()
        Long::class.qualifiedName -> value.toLong()
        Float::class.qualifiedName -> value.toFloat()
        Double::class.qualifiedName -> value.toDouble()
        String::class.qualifiedName -> value
        JBColor::class.qualifiedName -> JBColor(value.toInt(), value.toInt())
        else -> parseValue(valueType, value)
      }
    }

    private fun parseValue(valueType: String, value: String): Any? {
      return try {
        val valueTypeClass = Class.forName(valueType, false, this::class.java.classLoader)
        if (valueTypeClass.isEnum) {
          valueTypeClass.enumConstants.first { Enum::class.cast(it).name == value }
        }
        else {
          error("Unsupported configuration property: $valueType")
        }
      } catch (e: Exception) {
        log.error("Failed to load class '$valueType' of value '$value'", e)
        null
      }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val instance: DeveloperToolsPluginService
      get() = ApplicationManager.getApplication().getService(DeveloperToolsPluginService::class.java)

    private val log = logger<DeveloperToolsPluginService>()

    private const val PROPERTY_TYPE_VALUE_DELIMITER = "|"
    private val SUPPORTED_TYPES = setOf<KClass<*>>(
      Boolean::class,
      Int::class,
      Long::class,
      Float::class,
      Double::class,
      String::class,
      JBColor::class
    )

    fun checkStateType(type: KClass<*>) {
      check(type.java.isEnum || SUPPORTED_TYPES.contains(type)) {
        "Unsupported configuration property type: ${type.qualifiedName}"
      }
    }

    private const val LOAD_EXAMPLES_DEFAULT = true
    private const val SAVE_INPUTS_DEFAULT = true

    val lastSelectedContentNodeId = instance.lastSelectedContentNodeId
    val loadExamples = instance.loadExamples
    val saveInputs = instance.saveInputs
  }
}
