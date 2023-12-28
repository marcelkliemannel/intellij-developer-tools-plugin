package dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.XCollection.Style.v2
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyContainer
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.SECRET
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings.DeveloperToolsApplicationSettings.Companion.saveConfigurations
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings.DeveloperToolsApplicationSettings.Companion.saveInputs
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings.DeveloperToolsApplicationSettings.Companion.saveSecrets
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty
import java.security.Provider
import java.security.Security
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

  init {
    try {
      val bouncyCastleProviderClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
      val bouncyCastleProvider = bouncyCastleProviderClass.getConstructor().newInstance()
      Security.addProvider(bouncyCastleProvider as Provider)
    } catch (e: Exception) {
      log.debug("Can't load BouncyCastleProvider", e)
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun setExpandedGroupNodeIds(expandedGroupNodeIds: Set<String>) {
    this.expandedGroupNodeIds = expandedGroupNodeIds.toMutableSet()
  }

  fun getDeveloperToolConfigurations(developerToolId: String): List<DeveloperToolConfiguration> =
    developerToolsConfigurations[developerToolId]?.toList() ?: emptyList()

  fun createDeveloperToolConfiguration(developerToolId: String): DeveloperToolConfiguration {
    val newDeveloperToolConfiguration = DeveloperToolConfiguration(name = "Workbench")
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
        developerToolConfigurations
          .filter { it.properties.any { (_, property) -> property.valueChanged() } }
          .map { createDeveloperToolConfigurationState(developerToolId, it) }
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
          persistentProperties = developerToolsConfigurationState.properties!!
            .asSequence()
            .filter { it.key != null && it.type != null }
            .filter { it.type != INPUT || saveInputs }
            .filter { it.type != CONFIGURATION || saveConfigurations }
            .mapNotNull { property ->
              restorePropertyValue(
                developerToolId = developerToolsConfigurationState.developerToolId!!,
                key = property.key!!,
                value = property.value,
                type = property.type!!
              )?.let { property.key!! to it }
            }.toMap()
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
  ) = DeveloperToolConfigurationState(
    developerToolId = developerToolId,
    id = developerToolConfiguration.id.toString(),
    name = developerToolConfiguration.name,
    properties = developerToolConfiguration
      .properties
      .filter { (_, property) -> property.valueChanged() }
      .filter { (_, property) -> !(!saveInputs && property.type != INPUT) }
      .filter { (_, property) -> !(!saveSecrets && property.type != SECRET) }
      .map { (key, property) ->
        storeProperty(developerToolId = developerToolId, key = key, property = property)
      }
  )

  private fun storeProperty(
    developerToolId: String,
    key: String,
    property: PropertyContainer
  ): DeveloperToolConfigurationProperty {
    return when (property.type) {
      CONFIGURATION, INPUT ->
        DeveloperToolConfigurationProperty(key = key, value = property.reference.get(), type = property.type)

      SECRET -> {
        val credentialAttribute = createPropertyCredentialAttribute(developerToolId = developerToolId, propertyKey = key)
        PasswordSafe.instance.set(credentialAttribute, Credentials(null, property.reference.get() as String))
        // The value is not stored in the XML file, but we still need the
        // `DeveloperToolConfigurationProperty` to represent the property.
        // Therefore we use `*******` as a dummy value.
        DeveloperToolConfigurationProperty(key = key, value = "*******", type = property.type)
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

        if (!saveSecrets) {
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
        else -> error("Unsupported configuration property type: ${value::class.qualifiedName}")
      }
      return "${valueType}$PROPERTY_TYPE_VALUE_DELIMITER$serializedValue"
    }

    /**
     * If the value can't be restored this method must return null. All
     * properties with null values will be filtered in
     * [DeveloperToolsInstanceSettings.loadState].
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
        LocaleContainer::class.qualifiedName -> LocaleContainer(Locale.forLanguageTag(value))
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

    private val log = logger<DeveloperToolsInstanceSettings>()

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
