package dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings

import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.XCollection.Style.v2
import com.jetbrains.rd.util.UUID
import com.jetbrains.rd.util.firstOrNull
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.PersistentProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.PropertyType
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.PropertyType.SENSITIVE
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import kotlin.reflect.cast

internal abstract class DeveloperToolsInstanceSettings {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val developerToolsConfigurations = ConcurrentHashMap<String, CopyOnWriteArrayList<DeveloperToolConfiguration>>()

  val lastSelectedContentNodeId: ValueProperty<String?> = ValueProperty(null)
  var expandedGroupNodeIds: MutableSet<String>? = null
    private set

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun setExpandedGroupNodeIds(expandedGroupNodeIds: Set<String>) {
    this.expandedGroupNodeIds = expandedGroupNodeIds.toMutableSet()
  }

  fun getDeveloperToolConfigurations(developerToolId: String): List<DeveloperToolConfiguration> =
    developerToolsConfigurations[developerToolId]?.toList() ?: emptyList()

  fun createDeveloperToolConfiguration(developerToolId: String): DeveloperToolConfiguration {
    val newDeveloperToolConfiguration = DeveloperToolConfiguration(
      name = "Workbench",
      id = UUID.randomUUID(),
      persistentProperties = emptyMap()
    )
    developerToolsConfigurations.compute(developerToolId) { _, developerToolConfigurations ->
      (developerToolConfigurations ?: CopyOnWriteArrayList()).also { it.add(newDeveloperToolConfiguration) }
    }
    return newDeveloperToolConfiguration
  }

  fun removeDeveloperToolConfiguration(developerToolId: String, developerToolConfiguration: DeveloperToolConfiguration) {
    developerToolsConfigurations[developerToolId]?.remove(developerToolConfiguration)
  }

  open fun getState(): InstanceState {
    val stateDeveloperToolsConfigurations = developerToolsConfigurations.asSequence()
      .flatMap { (developerToolId, developerToolConfigurations) ->
        developerToolConfigurations.mapNotNull { createDeveloperToolConfigurationState(developerToolId, it) }
      }.toList()

    return InstanceState(
      developerToolsConfigurations = stateDeveloperToolsConfigurations,
      lastSelectedContentNodeId = lastSelectedContentNodeId.get(),
      expandedGroupNodeIds = expandedGroupNodeIds?.toList()
    )
  }

  open fun loadState(state: InstanceState) {
    lastSelectedContentNodeId.set(state.lastSelectedContentNodeId)
    setExpandedGroupNodeIds(state.expandedGroupNodeIds?.toSet() ?: emptySet())

    developerToolsConfigurations.clear()
    state.developerToolsConfigurations
      ?.filter { it.developerToolId != null && it.id != null && it.name != null && it.properties != null }
      ?.forEach { developerToolsConfigurationState ->
        val developerToolConfiguration = DeveloperToolConfiguration(
          name = developerToolsConfigurationState.name!!,
          id = UUID.fromString(developerToolsConfigurationState.id!!),
          persistentProperties = developerToolsConfigurationState.properties
            ?.asSequence()
            // The `type` property can be `null` in cases in which a type was
            // removed from the `PropertyType` enum.
            ?.filter { it.key != null && it.type != null && it.value != null }
            ?.filter { shouldSavePropertyType(it.type) }
            ?.map { it.key!! to PersistentProperty(it.key!!, it.value!!, it.type!!) }
            ?.toMap() ?: emptyMap()
        )

        developerToolsConfigurations.compute(developerToolsConfigurationState.developerToolId!!) { _, developerToolConfigurations ->
          (developerToolConfigurations ?: CopyOnWriteArrayList()).also { it.add(developerToolConfiguration) }
        }
      }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun shouldSavePropertyType(propertyType: PropertyType?): Boolean =
    when (propertyType) {
      CONFIGURATION -> DeveloperToolsApplicationSettings.instance.saveConfigurations
      INPUT -> DeveloperToolsApplicationSettings.instance.saveInputs
      SENSITIVE -> DeveloperToolsApplicationSettings.instance.saveSensitiveInputs
      null -> false
    }

  private fun createDeveloperToolConfigurationState(
    developerToolId: String,
    developerToolConfiguration: DeveloperToolConfiguration
  ): DeveloperToolConfigurationState? {
    val properties = if (developerToolConfiguration.wasConsumedByDeveloperTool) {
      developerToolConfiguration.properties
        .filter { (_, property) -> property.valueWasChanged() }
        .map { PersistentProperty(key = it.key, value = it.value.reference.get(), it.value.type) }
    }
    else {
      developerToolConfiguration.persistentProperties.values
    }
    if (properties.isEmpty()) {
      return null
    }

    return DeveloperToolConfigurationState(
      developerToolId = developerToolId,
      id = developerToolConfiguration.id.toString(),
      name = developerToolConfiguration.name,
      properties = properties
        .filter { shouldSavePropertyType(it.type) }
        .map { DeveloperToolConfigurationProperty(key = it.key, value = it.value, type = it.type) }
    )
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  open class InstanceState(
    @get:XCollection(style = v2, elementName = "developerToolsConfigurations")
    var developerToolsConfigurations: List<DeveloperToolConfigurationState>? = null,
    @get:Attribute("lastSelectedContentNodeId")
    var lastSelectedContentNodeId: String? = null,
    @get:XCollection(style = v2, elementName = "expandedGroupNodeId")
    var expandedGroupNodeIds: List<String>? = null
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @Tag(value = "developerToolConfiguration")
  data class DeveloperToolConfigurationState(
    @get:Attribute("developerToolId")
    var developerToolId: String? = null,
    @get:Attribute("id")
    var id: String? = null,
    @get:Attribute("name")
    var name: String? = null,
    @get:XCollection(style = v2, elementName = "properties")
    var properties: List<DeveloperToolConfigurationProperty>? = null,
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @Tag(value = "property")
  data class DeveloperToolConfigurationProperty(
    @get:Attribute("key")
    var key: String? = null,
    @get:Attribute("value", converter = StatePropertyValueConverter::class)
    var value: Any? = null,
    @get:Attribute("type", converter = PropertyTypeConverter::class)
    var type: PropertyType? = null
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  /**
   * Using a dedicated converter to handle removed values from the [PropertyType].
   */
  class PropertyTypeConverter : Converter<PropertyType>() {

    override fun fromString(value: String): PropertyType? {
      return PropertyType.entries.firstOrNull { it.name == value }
    }

    override fun toString(value: PropertyType): String {
      return value.name
    }
  }

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
        is LocaleContainer -> value.locale.toLanguageTag() to LocaleContainer::class.qualifiedName!!
        is BigDecimal -> value.toString() to BigDecimal::class.qualifiedName!!
        else -> error("Unsupported configuration property type: ${value::class.qualifiedName}")
      }
      return "${valueType}$PROPERTY_TYPE_VALUE_DELIMITER$serializedValue"
    }

    /**
     * If the value can't be restored, this method must return null.
     * All properties with null values will be filtered in
     * [DeveloperToolsInstanceSettings.loadState].
     */
    override fun fromString(value: String): Any? {
      val valueAndType = value.split(PROPERTY_TYPE_VALUE_DELIMITER, limit = 2)
      check(valueAndType.size == 2) { "Malformed serialized value: $value" }
      val valueType = applyTypeLegacy(valueAndType[0])
      val actualValue = valueAndType[1]
      return when (valueType) {
        Boolean::class.qualifiedName -> actualValue.toBoolean()
        Int::class.qualifiedName -> actualValue.toInt()
        Long::class.qualifiedName -> actualValue.toLong()
        Float::class.qualifiedName -> actualValue.toFloat()
        Double::class.qualifiedName -> actualValue.toDouble()
        String::class.qualifiedName -> actualValue
        JBColor::class.qualifiedName -> JBColor(actualValue.toInt(), actualValue.toInt())
        LocaleContainer::class.qualifiedName -> LocaleContainer(Locale.forLanguageTag(actualValue))
        BigDecimal::class.qualifiedName -> BigDecimal(actualValue)
        else -> parseValue(valueType, actualValue)
      }
    }

    private fun applyTypeLegacy(type: String): String {
      val legacyToApply = pre320TypePackageLegacy.filter { type.startsWith(it.key) }.firstOrNull()
        ?: return type

      return "${legacyToApply.value}${type.substring(legacyToApply.key.length)}"
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
        log.warn("Failed to load class '$valueType' of value '$value'", e)
        null
      }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val log = logger<DeveloperToolsInstanceSettings>()

    private val pre320TypePackageLegacy = mapOf(
      "dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool." to "dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.",
      "dev.turingcomplete.intellijdevelopertoolsplugins._internal.common." to "dev.turingcomplete.intellijdevelopertoolsplugin._internal.common."
    )
    private const val PROPERTY_TYPE_VALUE_DELIMITER = "|"
    private val SUPPORTED_TYPES = setOf<KClass<*>>(
      Boolean::class,
      Int::class,
      Long::class,
      Float::class,
      Double::class,
      String::class,
      JBColor::class,
      LocaleContainer::class,
      BigDecimal::class
    )

    fun <T : Any> assertPersistableType(type: KClass<T>): KClass<T> {
      check(type.java.isEnum || SUPPORTED_TYPES.contains(type)) {
        "Unsupported configuration/input property type: ${type.qualifiedName}"
      }
      return type
    }
  }
}
