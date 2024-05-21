package dev.turingcomplete.intellijdevelopertoolsplugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.SECRET
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty.Companion.RESET_CHANGE_ID
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsInstanceSettings.Companion.assertPersistableType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.safeCast

class DeveloperToolConfiguration(
  var name: String,
  val id: UUID,
  val persistentProperties: Map<String, PersistentProperty>
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  internal val properties = ConcurrentHashMap<String, PropertyContainer>()

  // If false, the `DeveloperToolFactory` never created a `DeveloperTool`
  // instance that could have register the current properties.
  // In this case, the `persistentProperties` need to be persisted again.
  internal var wasConsumedByDeveloperTool = false
  private val changeListeners = CopyOnWriteArrayList<ChangeListener>()
  private val resetListeners = CopyOnWriteArrayList<ResetListener>()
  var isResetting = false
    internal set

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun <T : Any> register(
    key: String,
    defaultValue: T,
    propertyType: PropertyType = CONFIGURATION,
    example: T? = null
  ): ValueProperty<T> =
    properties[key]?.let { reuseExistingProperty(it) } ?: createNewProperty(defaultValue, propertyType, key, createExampleProvider(example))

  fun <T : Any> registerWithExampleProvider(
    key: String,
    defaultValue: T,
    propertyType: PropertyType = CONFIGURATION,
    example: (() -> T)? = null
  ): ValueProperty<T> =
    properties[key]?.let { reuseExistingProperty(it) } ?: createNewProperty(defaultValue, propertyType, key, example)

  fun addChangeListener(parentDisposable: Disposable, changeListener: ChangeListener) {
    changeListeners.add(changeListener)
    Disposer.register(parentDisposable) { changeListeners.remove(changeListener) }
  }

  fun removeChangeListener(changeListener: ChangeListener) {
    changeListeners.remove(changeListener)
  }

  fun addResetListener(parentDisposable: Disposable, resetListener: ResetListener) {
    resetListeners.add(resetListener)
    Disposer.register(parentDisposable) { resetListeners.remove(resetListener) }
  }

  fun removeResetListener(resetListener: ResetListener) {
    resetListeners.remove(resetListener)
  }

  fun reset(
    type: PropertyType? = null,
    loadExamples: Boolean = DeveloperToolsApplicationSettings.instance.loadExamples
  ) {
    isResetting = true
    try {
      properties.filter { type == null || it.value.type == type }
        .forEach { (_, property) ->
          property.reset(loadExamples)
          fireConfigurationChanged(property.reference)
        }
      resetListeners.forEach { it.configurationReset() }
    }
    finally {
      isResetting = false
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun <T : Any> createExampleProvider(example: T?) =
    if (example != null) {
      { example }
    }
    else {
      null
    }

  private fun <T : Any> reuseExistingProperty(property: PropertyContainer): ValueProperty<T> {
    if ((property.type == INPUT && !DeveloperToolsApplicationSettings.instance.saveInputs)
      || (property.type == CONFIGURATION && !DeveloperToolsApplicationSettings.instance.saveConfigurations)
      || (property.type == SECRET && !DeveloperToolsApplicationSettings.instance.saveSecrets)
    ) {
      property.reset(DeveloperToolsApplicationSettings.instance.loadExamples)
    }

    @Suppress("UNCHECKED_CAST")
    return property.reference as ValueProperty<T>
  }

  private fun <T : Any> createNewProperty(
    defaultValue: T,
    propertyType: PropertyType,
    key: String,
    example: (() -> T)?
  ): ValueProperty<T> {
    val type = assertPersistableType(defaultValue::class, propertyType)
    val existingPropertyValue = persistentProperties[key]?.value
    val initialValue: T = type.safeCast(existingPropertyValue) ?: let {
      if (DeveloperToolsApplicationSettings.instance.loadExamples && example != null) example() else defaultValue
    }
    val valueProperty = ValueProperty(initialValue).apply {
      afterChangeConsumeEvent(null, handlePropertyChange(key))
    }
    properties[key] = PropertyContainer(
      key = key,
      reference = valueProperty,
      defaultValue = defaultValue,
      example = example,
      type = propertyType
    )
    return valueProperty
  }

  private fun fireConfigurationChanged(property: ValueProperty<out Any>) {
    changeListeners.forEach { it.configurationChanged(property) }
  }

  private fun <T : Any?> handlePropertyChange(key: String): (ValueProperty.ChangeEvent<T>) -> Unit = { event ->
    val newValue = event.newValue
    if (event.oldValue != newValue) {
      properties[key]?.let { property ->
        fireConfigurationChanged(property.reference)
      } ?: error("Unknown property: $key")
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal data class PropertyContainer(
    val key: String,
    val reference: ValueProperty<out Any>,
    val defaultValue: Any,
    val example: (() -> Any)?,
    val type: PropertyType
  ) {

    fun reset(loadExamples: Boolean) {
      val value = if (example != null && loadExamples) example.invoke() else defaultValue
      reference.setWithUncheckedCast(value, RESET_CHANGE_ID)
    }

    fun valueIsNotDefaultOrExample(): Boolean {
      val value = reference.get()
      return defaultValue != value && example != value
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class PropertyType {

    CONFIGURATION,
    INPUT,
    SECRET,
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @FunctionalInterface
  fun interface ChangeListener {

    fun configurationChanged(property: ValueProperty<out Any>)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @FunctionalInterface
  fun interface ResetListener {

    fun configurationReset()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class PersistentProperty(val key: String, val value: Any, val type: PropertyType)

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}