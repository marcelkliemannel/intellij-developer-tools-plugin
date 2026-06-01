package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.XCollection.Style.v2
import com.jetbrains.rd.util.UUID
import dev.turingcomplete.intellijdevelopertoolsplugin.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginInfo
import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginInfo.PluginVersion.Companion.toPluginVersion
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PersistentProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.SENSITIVE
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfigurationPropertyType.SimplePropertyType
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.generalSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.InstanceState
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettingsLegacy.applyConfigurationPropertyKeyLegacies
import java.math.BigDecimal
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

abstract class DeveloperToolsInstanceSettings : PersistentStateComponent<InstanceState> {
  // -- Properties ---------------------------------------------------------- //

  private val developerToolsConfigurations =
    ConcurrentHashMap<String, CopyOnWriteArrayList<DeveloperToolConfiguration>>()

  val lastSelectedContentNodeId: ValueProperty<String?> = ValueProperty(null)
  var expandedGroupNodeIds: MutableSet<String>? = null
    private set

  private val tabOrders =
    ConcurrentHashMap<String, MutableList<String>>()

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun setExpandedGroupNodeIds(expandedGroupNodeIds: Set<String>) {
    this.expandedGroupNodeIds = expandedGroupNodeIds.toMutableSet()
  }

  fun getDeveloperToolConfigurations(developerToolId: String): List<DeveloperToolConfiguration> =
    developerToolsConfigurations[developerToolId]?.toList() ?: emptyList()

  fun createDeveloperToolConfiguration(developerToolId: String): DeveloperToolConfiguration {
    val newDeveloperToolConfiguration =
      DeveloperToolConfiguration(
        name = "Workbench",
        id = UUID.randomUUID(),
        persistentProperties = emptyMap(),
      )
    developerToolsConfigurations.compute(developerToolId) { _, developerToolConfigurations ->
      (developerToolConfigurations ?: CopyOnWriteArrayList()).also {
        it.add(newDeveloperToolConfiguration)
      }
    }
    return newDeveloperToolConfiguration
  }

  fun removeDeveloperToolConfiguration(
    developerToolId: String,
    developerToolConfiguration: DeveloperToolConfiguration,
  ) {
    developerToolsConfigurations[developerToolId]?.remove(developerToolConfiguration)
  }

  fun getToolTabOrder(toolId: String): List<String> =
    tabOrders[toolId]?.toList() ?: emptyList()

  fun setToolTabOrder(toolId: String, order: List<String>) {
    tabOrders[toolId] = order.toMutableList()
  }

  override fun getState(): InstanceState {
    val stateDeveloperToolsConfigurations =
      developerToolsConfigurations
        .asSequence()
        .flatMap { (developerToolId, developerToolConfigurations) ->
          developerToolConfigurations.mapNotNull {
            createDeveloperToolConfigurationState(developerToolId, it)
          }
        }
        .toList()

    return InstanceState(
      pluginVersion = PluginInfo.pluginVersion.toString(),
      developerToolsConfigurations = stateDeveloperToolsConfigurations,
      lastSelectedContentNodeId = lastSelectedContentNodeId.get(),
      expandedGroupNodeIds = expandedGroupNodeIds?.toList(),
      tabOrders = tabOrders.mapValues { it.value.toList() },
    )
  }

  override fun loadState(state: InstanceState) {
    lastSelectedContentNodeId.set(state.lastSelectedContentNodeId)
    setExpandedGroupNodeIds(state.expandedGroupNodeIds?.toSet() ?: emptySet())

    val statePluginVersion: PluginInfo.PluginVersion? = state.pluginVersion?.toPluginVersion()

    developerToolsConfigurations.clear()
    state.developerToolsConfigurations
      ?.filter {
        it.developerToolId != null && it.id != null && it.name != null && it.properties != null
      }
      ?.forEach { developerToolsConfigurationState ->
        val developerToolConfiguration =
          DeveloperToolConfiguration(
            name = developerToolsConfigurationState.name!!,
            id = UUID.fromString(developerToolsConfigurationState.id!!),
            persistentProperties =
              developerToolsConfigurationState.properties
                ?.asSequence()
                // The `type` property can be `null` in cases in which a type was
                // removed from the `PropertyType` enum.
                ?.filter { it.key != null && it.type != null && it.value != null }
                ?.filter { shouldSavePropertyType(it.type!!) }
                ?.map {
                  val propertyKey =
                    applyConfigurationPropertyKeyLegacies(
                      statePluginVersion = statePluginVersion,
                      developerToolId = developerToolsConfigurationState.developerToolId!!,
                      propertyKey = it.key!!,
                    )
                  propertyKey to PersistentProperty(propertyKey, it.value!!, it.type!!)
                }
                ?.toMap() ?: emptyMap(),
          )

        developerToolsConfigurations.compute(developerToolsConfigurationState.developerToolId!!) {
          _,
          developerToolConfigurations ->
          (developerToolConfigurations ?: CopyOnWriteArrayList()).also {
            it.add(developerToolConfiguration)
          }
        }
      }

    tabOrders.clear()
    state.tabOrders?.forEach { (toolId, order) ->
      tabOrders[toolId] = order.toMutableList()
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun shouldSavePropertyType(propertyType: PropertyType): Boolean =
    when (propertyType) {
      CONFIGURATION -> generalSettings.saveConfigurations.get()
      INPUT -> generalSettings.saveInputs.get()
      SENSITIVE -> generalSettings.saveSensitiveInputs.get()
    }

  private fun createDeveloperToolConfigurationState(
    developerToolId: String,
    developerToolConfiguration: DeveloperToolConfiguration,
  ): DeveloperToolConfigurationState? {
    val properties =
      if (developerToolConfiguration.wasConsumedByDeveloperTool) {
        developerToolConfiguration.properties
          .filter { (_, property) -> property.valueWasChanged() }
          .map { PersistentProperty(key = it.key, value = it.value.reference.get(), it.value.type) }
      } else {
        developerToolConfiguration.persistentProperties.values
      }
    if (properties.isEmpty()) {
      return null
    }

    return DeveloperToolConfigurationState(
      developerToolId = developerToolId,
      id = developerToolConfiguration.id.toString(),
      name = developerToolConfiguration.name,
      properties =
        properties
          .filter { shouldSavePropertyType(it.type) }
          .map {
            DeveloperToolConfigurationProperty(key = it.key, value = it.value, type = it.type)
          },
    )
  }

  // -- Inner Type ---------------------------------------------------------- //

  open class InstanceState(
    @get:Attribute("pluginVersion") var pluginVersion: String? = null,
    @get:XCollection(style = v2, elementName = "developerToolsConfigurations")
    var developerToolsConfigurations: List<DeveloperToolConfigurationState>? = null,
    @get:Attribute("lastSelectedContentNodeId") var lastSelectedContentNodeId: String? = null,
    @get:XCollection(style = v2, elementName = "expandedGroupNodeId")
    var expandedGroupNodeIds: List<String>? = null,
    @get:XCollection(style = v2, elementName = "tabOrder")
    var tabOrders: Map<String, List<String>>? = null,
  )

  // -- Inner Type ---------------------------------------------------------- //

  @Tag(value = "developerToolConfiguration")
  data class DeveloperToolConfigurationState(
    @get:Attribute("developerToolId") var developerToolId: String? = null,
    @get:Attribute("id") var id: String? = null,
    @get:Attribute("name") var name: String? = null,
    @get:XCollection(style = v2, elementName = "properties")
    var properties: List<DeveloperToolConfigurationProperty>? = null,
  )

  // -- Inner Type ---------------------------------------------------------- //

  @Tag(value = "property")
  data class DeveloperToolConfigurationProperty(
    @get:Attribute("key") var key: String? = null,
    @get:Attribute("value", converter = StatePropertyValueConverter::class) var value: Any? = null,
    @get:Attribute("type", converter = PropertyTypeConverter::class) var type: PropertyType? = null,
  )

  // -- Inner Type ---------------------------------------------------------- //

  /** Using a dedicated converter to handle removed values from the [PropertyType]. */
  class PropertyTypeConverter : Converter<PropertyType>() {

    override fun fromString(value: String): PropertyType? {
      return PropertyType.entries.firstOrNull { it.name == value }
    }

    override fun toString(value: PropertyType): String {
      return value.name
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class StatePropertyValueConverter : Converter<Any>() {

    override fun toString(value: Any): String {
      val configurationPropertyType =
        configurationConfigurationPropertyTypes[value::class]
          ?: error("Unsupported configuration property type: ${value::class.qualifiedName}")
      val name = configurationPropertyType.id
      val persistedValue = configurationPropertyType.toPersistent(value)
      return "$name$PROPERTY_TYPE_VALUE_DELIMITER$persistedValue"
    }

    /**
     * If the value can't be restored, this method must return null. All properties with null values
     * will be filtered in [DeveloperToolsInstanceSettings.loadState].
     */
    override fun fromString(value: String): Any? {
      return try {
        val valueAndType = value.split(PROPERTY_TYPE_VALUE_DELIMITER, limit = 2)
        check(valueAndType.size == 2) { "Malformed serialized value: $value" }
        val valueType = valueAndType[0]
        val actualValue = valueAndType[1]
        val configurationPropertyType =
          configurationPropertyTypesByNamesAndLegacyValueTypes[valueType]
        return if (configurationPropertyType == null) {
          log.warn("Missing property type: $valueType")
          null
        } else {
          configurationPropertyType.fromPersistent(actualValue)
        }
      } catch (e: Exception) {
        log.warn("Failed to load configuration property of value: $value", e)
        null
      }
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val log = logger<DeveloperToolsInstanceSettings>()

    private const val PROPERTY_TYPE_VALUE_DELIMITER = "|"

    fun <T : Any> assertPersistableType(type: KClass<T>): KClass<T> {
      check(configurationConfigurationPropertyTypes.contains(type)) {
        "Unsupported configuration property type: ${type.qualifiedName}"
      }
      return type
    }

    private val configurationConfigurationPropertyTypes:
      Map<KClass<*>, DeveloperToolConfigurationPropertyType<*>> by lazy {
      builtInConfigurationPropertyTypes + loadEnumConfigurationPropertyTypes()
    }

    val configurationPropertyTypesByNamesAndLegacyValueTypes:
      Map<String, DeveloperToolConfigurationPropertyType<*>> by lazy {
      configurationConfigurationPropertyTypes.values
        .flatMap { it ->
          val propertyTypes = mutableListOf(it.id to it)
          it.legacyId?.let { legacyName -> propertyTypes.add(legacyName to it) }
          return@flatMap propertyTypes
        }
        .toMap()
    }

    private fun loadEnumConfigurationPropertyTypes():
      Map<KClass<*>, DeveloperToolConfigurationPropertyType<*>> =
      DeveloperToolConfigurationEnumPropertyTypeEp.epName.extensions.asSequence().associateBy {
        it.typeClass
      }

    val builtInConfigurationPropertyTypes:
      Map<KClass<*>, DeveloperToolConfigurationPropertyType<*>> =
      listOf(
          SimplePropertyType(
            id = Boolean::class.qualifiedName!!,
            typeClass = Boolean::class,
            doFromPersistent = { it.toBoolean() },
            doToPersistent = { it.toString() },
          ),
          SimplePropertyType(
            id = Int::class.qualifiedName!!,
            typeClass = Int::class,
            doFromPersistent = { it.toInt() },
            doToPersistent = { it.toString() },
          ),
          SimplePropertyType(
            id = Long::class.qualifiedName!!,
            typeClass = Long::class,
            doFromPersistent = { it.toLong() },
            doToPersistent = { it.toString() },
          ),
          SimplePropertyType(
            id = Float::class.qualifiedName!!,
            typeClass = Float::class,
            doFromPersistent = { it.toFloat() },
            doToPersistent = { it.toString() },
          ),
          SimplePropertyType(
            id = Double::class.qualifiedName!!,
            typeClass = Double::class,
            doFromPersistent = { it.toDouble() },
            doToPersistent = { it.toString() },
          ),
          SimplePropertyType(
            id = String::class.qualifiedName!!,
            typeClass = String::class,
            doFromPersistent = { it },
            doToPersistent = { it },
          ),
          SimplePropertyType(
            id = JBColor::class.qualifiedName!!,
            typeClass = JBColor::class,
            doFromPersistent = { JBColor(it.toInt(), it.toInt()) },
            doToPersistent = { it.rgb.toString() },
          ),
          SimplePropertyType(
            id = LocaleContainer::class.qualifiedName!!,
            typeClass = LocaleContainer::class,
            doFromPersistent = { LocaleContainer(Locale.forLanguageTag(it)) },
            doToPersistent = { it.locale.toLanguageTag() },
            legacyId =
              "dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.LocaleContainer",
          ),
          SimplePropertyType(
            id = BigDecimal::class.qualifiedName!!,
            typeClass = BigDecimal::class,
            doFromPersistent = { BigDecimal(it) },
            doToPersistent = { it.toString() },
          ),
        )
        .associateBy { it.typeClass }
  }
}
