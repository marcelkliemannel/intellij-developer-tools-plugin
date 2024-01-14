package dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.XCollection.Style.v2
import com.jetbrains.rd.util.UUID
import com.jetbrains.rd.util.firstOrNull
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PersistentProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.SECRET
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
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
            ?.filter { it.key != null && it.type != null }
            ?.filter { it.type != INPUT || DeveloperToolsApplicationSettings.instance.saveInputs }
            ?.filter { it.type != CONFIGURATION || DeveloperToolsApplicationSettings.instance.saveConfigurations }
            ?.mapNotNull { property ->
              restorePropertyValue(
                developerToolId = developerToolsConfigurationState.developerToolId!!,
                key = property.key!!,
                value = property.value,
                type = property.type!!
              )?.let {
                val key = property.key!!
                key to PersistentProperty(key, it, property.type!!)
              }
            }
            ?.toMap() ?: emptyMap()
        )

        developerToolsConfigurations.compute(developerToolsConfigurationState.developerToolId!!) { _, developerToolConfigurations ->
          (developerToolConfigurations ?: CopyOnWriteArrayList()).also { it.add(developerToolConfiguration) }
        }
      }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createDeveloperToolConfigurationState(
    developerToolId: String,
    developerToolConfiguration: DeveloperToolConfiguration
  ): DeveloperToolConfigurationState? {
    val properties = if (developerToolConfiguration.wasConsumedByDeveloperTool) {
      developerToolConfiguration.properties
        .filter { (_, property) -> property.valueIsNotDefaultOrExample() }
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
        .filter { !(!DeveloperToolsApplicationSettings.instance.saveInputs && it.type != INPUT) }
        .filter { !(!DeveloperToolsApplicationSettings.instance.saveSecrets && it.type != SECRET) }
        .map { storeProperty(developerToolId = developerToolId, key = it.key, property = it) }
    )
  }

  private fun storeProperty(
    developerToolId: String,
    key: String,
    property: PersistentProperty
  ): DeveloperToolConfigurationProperty {
    return when (property.type) {
      CONFIGURATION, INPUT ->
        DeveloperToolConfigurationProperty(key = key, value = property.value, type = property.type)

      SECRET -> {
        val credentialAttribute = createPropertyCredentialAttribute(developerToolId = developerToolId, propertyKey = key)
        ApplicationManager.getApplication().executeOnPooledThread {
          // Not allowed to be executed on the EDT
          PasswordSafe.instance.set(credentialAttribute, Credentials(null, property.value as String))
        }
        // The value is not stored in the XML file, but we still need the
        // `DeveloperToolConfigurationProperty` to represent the property.
        // Therefore, we use `SAVED_IN_KEYSTORE` as a placeholder value.
        DeveloperToolConfigurationProperty(key = key, value = "SAVED_IN_KEYSTORE", type = property.type)
      }
    }
  }

  private fun restorePropertyValue(
    developerToolId: String,
    key: String,
    value: Any?,
    type: PropertyType
  ): Any? {
    return when (type) {
      CONFIGURATION, INPUT ->
        value

      SECRET -> {
        val credentialAttribute = createPropertyCredentialAttribute(developerToolId = developerToolId, propertyKey = key)
        val secretValue = PasswordSafe.instance.getPassword(credentialAttribute)

        if (!DeveloperToolsApplicationSettings.instance.saveSecrets) {
          if (secretValue != null) {
            // Remove the secret from the password safe
            PasswordSafe.instance.set(credentialAttribute, null)
          }
          else {
            null
          }
        }
        else {
          secretValue
        }
      }
    }
  }

  private fun createPropertyCredentialAttribute(
    developerToolId: String,
    propertyKey: String
  ): CredentialAttributes {
    val credentialKey = "$developerToolId-$propertyKey"
    return CredentialAttributes(
      serviceName = generateServiceName("Developer Tools Plugin", credentialKey),
      userName = credentialKey
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
    @get:Attribute("type")
    var type: PropertyType? = null
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
    override fun fromString(serializedValue: String): Any? {
      val valueAndType = serializedValue.split(PROPERTY_TYPE_VALUE_DELIMITER, limit = 2)
      check(valueAndType.size == 2) { "Malformed serialized value: $serializedValue" }
      val valueType = applyTypeLegacy(valueAndType[0])
      val value = valueAndType[1]
      return when (valueType) {
        Boolean::class.qualifiedName -> value.toBoolean()
        Int::class.qualifiedName -> value.toInt()
        Long::class.qualifiedName -> value.toLong()
        Float::class.qualifiedName -> value.toFloat()
        Double::class.qualifiedName -> value.toDouble()
        String::class.qualifiedName -> value
        JBColor::class.qualifiedName -> JBColor(value.toInt(), value.toInt())
        LocaleContainer::class.qualifiedName -> LocaleContainer(Locale.forLanguageTag(value))
        BigDecimal::class.qualifiedName -> BigDecimal(value)
        else -> parseValue(valueType, value)
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

    fun <T : Any> assertPersistableType(type: KClass<T>, propertyType: PropertyType): KClass<T> {
      when (propertyType) {
        CONFIGURATION, INPUT -> {
          check(type.java.isEnum || SUPPORTED_TYPES.contains(type)) {
            "Unsupported configuration/input property type: ${type.qualifiedName}"
          }
        }

        SECRET -> {
          check(type::class != String::class) {
            "Unsupported secret property type: ${type.qualifiedName}"
          }
        }
      }

      return type
    }
  }
}
