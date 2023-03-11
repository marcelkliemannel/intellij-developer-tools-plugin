package dev.turingcomplete.intellijdevelopertoolsplugins

import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService.Companion.checkStateType
import io.ktor.util.reflect.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class DeveloperToolConfiguration {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  internal val properties = ConcurrentHashMap<String, Any>()
  private val changeListeners = CopyOnWriteArrayList<ChangeListener>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun <T : Any> register(key: String, defaultValue: T): ReadWriteProperty<Any?, T> {
    checkStateType(defaultValue::class)

    @Suppress("UNCHECKED_CAST")
    val initialValue : T = if (properties.containsKey(key)) (properties[key] as T) else defaultValue
    properties[key] = initialValue
    return Delegates.observable(initialValue, handleChange(key, defaultValue))
  }

  fun addChangeListener(changeListener: ChangeListener) {
    changeListeners.add(changeListener)
  }

  fun removeChangeListener(changeListener: ChangeListener) {
    changeListeners.remove(changeListener)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun <T : Any> handleChange(key: String, defaultValue: T): (KProperty<*>, T, T) -> Unit = { _, old, new ->
    if (old != new) {
      if (new != defaultValue) {
        properties[key] = new as Any
      }
      else {
        properties.remove(key)
      }
      changeListeners.forEach { it.configurationChanged(key) }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  interface ChangeListener {

    fun configurationChanged(key: String)
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}