package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.ui.layout.ComponentPredicate

class PropertyComponentPredicate<T>(
  private val property: ObservableProperty<T>,
  private val expectedValue: T
) : ComponentPredicate() {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val listeners = mutableListOf<(T) -> Unit>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    property.afterChange { value ->
      listeners.forEach { it(value) }
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun addListener(listener: (Boolean) -> Unit) {
    listeners.add { value ->
      listener(value?.equals(expectedValue) ?: false)
    }
  }

  override fun invoke(): Boolean = property.get()?.equals(expectedValue) ?: false

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}