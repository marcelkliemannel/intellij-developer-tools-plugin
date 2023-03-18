package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.util.Disposer
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService.Companion.checkStateType
import io.ktor.util.reflect.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class DeveloperToolConfiguration {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  internal val properties = ConcurrentHashMap<String, Any>()
  private val changeListeners = CopyOnWriteArrayList<ChangeListener>()
  private val bulkChangeInProgress = AtomicBoolean(false)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun <T : Any> register(key: String, defaultValue: T): ObservableMutableProperty<T> {
    checkStateType(defaultValue::class)

    @Suppress("UNCHECKED_CAST")
    val initialValue: T = if (properties.containsKey(key)) (properties[key] as T) else defaultValue
    properties[key] = initialValue
    return AtomicProperty(initialValue).apply {
      afterChange { newValue ->
        val oldValue = properties[key]
        properties[key] = newValue
        if (newValue != oldValue && !bulkChangeInProgress.get()) {
          fireConfigurationChanged()
        }
      }
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

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @FunctionalInterface
  interface ChangeListener {

    fun configurationChanged()
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}