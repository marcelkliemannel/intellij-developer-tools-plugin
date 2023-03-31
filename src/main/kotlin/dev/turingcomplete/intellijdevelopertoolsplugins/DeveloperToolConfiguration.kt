@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.util.Disposer
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService.Companion.checkStateType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

class DeveloperToolConfiguration {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  internal val properties = ConcurrentHashMap<String, Any?>()
  private val changeListeners = CopyOnWriteArrayList<ChangeListener>()
  private val bulkChangeInProgress = AtomicBoolean(false)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun <T : Any> register(key: String, defaultValue: T): ObservableMutableProperty<T> =
    register(key, defaultValue, defaultValue::class)

  fun <T : Any?> registerNullable(key: String, defaultValue: T, valueType: KClass<*>): ObservableMutableProperty<T?> {
    checkStateType(valueType::class)

    val initialValue: T? = initializeProperty(key, defaultValue)
    return AtomicProperty(initialValue).apply {
      afterChange { newValue -> handlePropertyChange<T>(key, newValue) }
    }
  }

  fun <T : Any> register(key: String, defaultValue: T, valueType: KClass<out T>): ObservableMutableProperty<T> {
    checkStateType(valueType)

    val initialValue: T = initializeProperty(key, defaultValue)
    return AtomicProperty(initialValue).apply {
      afterChange { newValue -> handlePropertyChange(key, newValue) }
    }
  }

  fun addChangeListener(parentDisposable: Disposable, changeListener: ChangeListener) {
    changeListeners.add(changeListener)
    Disposer.register(parentDisposable) { changeListeners.remove(changeListener) }
  }

  fun removeChangeListener(changeListener: ChangeListener) {
    changeListeners.remove(changeListener)
  }

  fun bulkChange(change: () -> Unit) {
    synchronized(bulkChangeInProgress) {
      bulkChangeInProgress.set(true)
      change()
      fireConfigurationChanged()
      bulkChangeInProgress.set(false)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun fireConfigurationChanged() {
    changeListeners.forEach { it.configurationChanged() }
  }

  private fun <T : Any?> initializeProperty(key: String, defaultValue: T): T {
    @Suppress("UNCHECKED_CAST")
    val initialValue: T = if (properties.containsKey(key)) (properties[key] as T) else defaultValue
    properties[key] = initialValue
    return initialValue
  }

  private fun <T : Any?> handlePropertyChange(key: String, newValue: T?) {
    val oldValue = properties[key]
    properties[key] = newValue
    if (newValue != oldValue && !bulkChangeInProgress.get()) {
      fireConfigurationChanged()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @FunctionalInterface
  interface ChangeListener {

    fun configurationChanged()
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}