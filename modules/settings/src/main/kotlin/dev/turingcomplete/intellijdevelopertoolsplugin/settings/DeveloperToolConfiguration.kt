package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.generalSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings.Companion.assertPersistableType
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.safeCast

class DeveloperToolConfiguration(
  var name: String,
  val id: UUID,
  val persistentProperties: Map<String, PersistentProperty>,
) {
  // -- Properties ---------------------------------------------------------- //

  val properties = ConcurrentHashMap<String, PropertyContainer>()

  // If false, the `DeveloperToolFactory` never created a `DeveloperTool`
  // instance that could have register the current properties.
  // In this case, the `persistentProperties` need to be persisted again.
  var wasConsumedByDeveloperTool = false
  private val changeListeners = CopyOnWriteArrayList<ChangeListener>()
  private val resetListeners = CopyOnWriteArrayList<ResetListener>()
  var isResetting = false
    private set

  private val favoriteProperty = register(
        "isFavorite",
        false,
        PropertyType.CONFIGURATION
    )

    var isFavorite: Boolean
        get() = favoriteProperty.get()
        set(value) {
            favoriteProperty.set(value)
        }



  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun <T : Any> register(
    key: String,
    defaultValue: T,
    propertyType: PropertyType = PropertyType.CONFIGURATION,
    example: T? = null,
  ): ConfigurationProperty<T> =
    properties[key]?.let { reuseExistingProperty(it) }
      ?: createNewProperty(defaultValue, propertyType, key, createExampleProvider(example))

  fun <T : Any> registerWithExampleProvider(
    key: String,
    defaultValue: T,
    propertyType: PropertyType = PropertyType.CONFIGURATION,
    example: (() -> T)? = null,
  ): ConfigurationProperty<T> =
    properties[key]?.let { reuseExistingProperty(it) }
      ?: createNewProperty(defaultValue, propertyType, key, example)

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
    loadExamples: Boolean = generalSettings.loadExamples.get(),
  ) {
    isResetting = true
    try {
      properties
        .filter { type == null || it.value.type == type }
        .forEach { (_, property) ->
          property.reset(loadExamples)
          fireConfigurationChanged(property.reference)
        }
      resetListeners.forEach { it.configurationReset() }
    } finally {
      isResetting = false
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun <T : Any> createExampleProvider(example: T?) =
    if (example != null) {
      { example }
    } else {
      null
    }

  private fun <T : Any> reuseExistingProperty(property: PropertyContainer): ValueProperty<T> {
    if (
      (property.type == PropertyType.INPUT && !generalSettings.saveInputs.get()) ||
        (property.type == PropertyType.CONFIGURATION &&
          !generalSettings.saveConfigurations.get()) ||
        (property.type == PropertyType.SENSITIVE && !generalSettings.saveSensitiveInputs.get())
    ) {
      property.reset(generalSettings.loadExamples.get())
    }

    @Suppress("UNCHECKED_CAST")
    return property.reference as ValueProperty<T>
  }

  private fun <T : Any> createNewProperty(
    defaultValue: T,
    propertyType: PropertyType,
    key: String,
    example: (() -> T)?,
  ): ValueProperty<T> {
    val type = assertPersistableType(defaultValue::class)
    val existingPropertyValue = persistentProperties[key]?.value
    val initialValue: T =
      type.safeCast(existingPropertyValue)
        ?: let {
          if (generalSettings.loadExamples.get() && example != null) example() else defaultValue
        }
    val valueProperty =
      ValueProperty(initialValue).apply { afterChangeConsumeEvent(null, handlePropertyChange(key)) }
    properties[key] =
      PropertyContainer(
        key = key,
        reference = valueProperty,
        defaultValue = defaultValue,
        example = example,
        type = propertyType,
      )
    return valueProperty
  }

  private fun fireConfigurationChanged(property: ValueProperty<out Any>) {
    changeListeners.forEach { it.configurationChanged(property) }
  }

  private fun <T : Any?> handlePropertyChange(key: String): (ValueProperty.ChangeEvent<T>) -> Unit =
    { event ->
      val newValue = event.newValue
      if (event.oldValue != newValue) {
        properties[key]?.let { property -> fireConfigurationChanged(property.reference) }
          ?: error("Unknown property: $key")
      }
    }

  // -- Inner Type ---------------------------------------------------------- //

  data class PropertyContainer(
    val key: String,
    val reference: ValueProperty<out Any>,
    val defaultValue: Any,
    val example: (() -> Any)?,
    val type: PropertyType,
  ) {

    fun reset(loadExamples: Boolean) {
      val value = if (example != null && loadExamples) example.invoke() else defaultValue
      reference.setWithUncheckedCast(value, RESET_CHANGE_ID)
    }

    fun valueWasChanged(): Boolean {
      val value = reference.get()
      val b =
        if (defaultValue is BigDecimal) {
          defaultValue.compareTo(value as BigDecimal) != 0 &&
            example?.invoke()?.uncheckedCastTo<BigDecimal>()?.compareTo(value)?.equals(0)?.not() !=
              false
        } else {
          defaultValue != value && example?.invoke()?.equals(value)?.not() != false
        }
      return b
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class PropertyType {

    CONFIGURATION,
    INPUT,
    SENSITIVE,
  }

  // -- Inner Type ---------------------------------------------------------- //

  @FunctionalInterface
  fun interface ChangeListener {

    fun configurationChanged(property: ValueProperty<out Any>)
  }

  // -- Inner Type ---------------------------------------------------------- //

  @FunctionalInterface
  fun interface ResetListener {

    fun configurationReset()
  }

  // -- Inner Type ---------------------------------------------------------- //

  data class PersistentProperty(val key: String, val value: Any, val type: PropertyType)

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    const val RESET_CHANGE_ID = "reset"
  }
}

typealias ConfigurationProperty<T> = ValueProperty<T>
