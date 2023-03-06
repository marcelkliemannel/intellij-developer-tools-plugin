package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.ui.layout.ComponentPredicate
import kotlin.properties.Delegates

class BooleanComponentPredicate(initialValue: Boolean) : ComponentPredicate() {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val listeners = mutableListOf<(Boolean) -> Unit>()

  var value: Boolean by Delegates.observable(initialValue) { _, oldValue, newValue ->
    if (oldValue != newValue) {
      listeners.forEach { it(newValue) }
    }
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun addListener(listener: (Boolean) -> Unit) {
    listeners.add(listener)
  }

  override fun invoke(): Boolean = value

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}