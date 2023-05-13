@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.SECRET
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService.Companion.assertPersistableType
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.uncheckedCastTo
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class DeveloperToolConfiguration(
  var name: String,
  val id: UUID = UUID.randomUUID(),
  val persistentProperties: Map<String, Any> = emptyMap()
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  internal val properties = ConcurrentHashMap<String, PropertyContainer>()
  private val changeListeners = CopyOnWriteArrayList<ChangeListener>()
  var isResetting = false
    internal set
  val hasChanges: Boolean
    get() = properties.values.any { it.valueChanged }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun <T : Any> register(
    key: String,
    defaultValue: T,
    propertyType: PropertyType = CONFIGURATION,
    example: T? = null
  ): ValueProperty<T> =
    properties[key]?.let { reuseExistingProperty(it) } ?: createNewProperty(defaultValue, propertyType, key, example)

  fun addChangeListener(parentDisposable: Disposable, changeListener: ChangeListener) {
    changeListeners.add(changeListener)
    Disposer.register(parentDisposable) { changeListeners.remove(changeListener) }
  }

  fun removeChangeListener(changeListener: ChangeListener) {
    changeListeners.remove(changeListener)
  }

  fun reset(
    type: PropertyType? = null,
    loadExamples: Boolean = DeveloperToolsPluginService.loadExamples
  ) {
    isResetting = true
    try {
      properties.filter { type == null || it.value.type == type }
        .forEach { (_, property) -> property.reset(loadExamples) }
      fireConfigurationChanged()
    }
    finally {
      isResetting = false
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun <T : Any> reuseExistingProperty(property: PropertyContainer): ValueProperty<T> {
    if ((property.type == INPUT && !DeveloperToolsPluginService.saveInputs)
      || (property.type == CONFIGURATION && !DeveloperToolsPluginService.saveConfiguration)
      || (property.type == SECRET && !DeveloperToolsPluginService.saveSecrets)
    ) {
      property.reset(DeveloperToolsPluginService.loadExamples)
    }

    @Suppress("UNCHECKED_CAST")
    return property.valueProperty as ValueProperty<T>
  }

  private fun <T : Any> createNewProperty(
    defaultValue: T,
    propertyType: PropertyType,
    key: String,
    example: T?
  ): ValueProperty<T> {
    val type = assertPersistableType(defaultValue::class, propertyType)
    val existingProperty = persistentProperties[key]
    val initialValue: T = existingProperty?.uncheckedCastTo(type) ?: let {
      if (DeveloperToolsPluginService.loadExamples && example != null) example else defaultValue
    }
    val valueProperty = ValueProperty(initialValue).apply {
      afterChangeConsumeEvent(null, handlePropertyChange(key))
    }
    properties[key] = PropertyContainer(
      key = key,
      valueProperty = valueProperty,
      defaultValue = defaultValue,
      example = example,
      type = propertyType,
      valueChanged = existingProperty != null
    )
    return valueProperty
  }

  private fun fireConfigurationChanged() {
    changeListeners.forEach { it.configurationChanged() }
  }

  private fun <T : Any?> handlePropertyChange(key: String): (ValueProperty.ChangeEvent<T>) -> Unit = { event ->
    val newValue = event.newValue
    if (event.oldValue != newValue) {
      properties[key]?.let { property ->
        property.valueChanged = property.defaultValue != newValue && property.example != newValue
        fireConfigurationChanged()
      } ?: error("Unknown property: $key")
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal data class PropertyContainer(
    val key: String,
    val valueProperty: ValueProperty<out Any>,
    val defaultValue: Any,
    val example: Any?,
    val type: PropertyType,
    var valueChanged: Boolean
  ) {

    fun reset(loadExamples: Boolean) {
      val value = if (example != null && loadExamples) example else defaultValue
      valueProperty.setWithUncheckedCast(value)
      valueChanged = false
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class PropertyType {

    CONFIGURATION,
    INPUT,
    SECRET,
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  interface ChangeListener {

    fun configurationChanged()
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}