package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.openapi.observable.dispatcher.SingleEventDispatcher
import com.intellij.ui.layout.ComponentPredicate
import kotlin.properties.Delegates

class BooleanComponentPredicate(initialValue: Boolean) : ComponentPredicate() {
  // -- Properties ---------------------------------------------------------- //

  private val changeDispatcher = SingleEventDispatcher.create<Boolean>()

  var value: Boolean by
    Delegates.observable(initialValue) { _, oldValue, newValue ->
      if (oldValue != newValue) {
        changeDispatcher.fireEvent(newValue)
      }
    }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun addListener(listener: (Boolean) -> Unit) {
    changeDispatcher.whenEventHappened(listener)
  }

  override fun invoke(): Boolean = value

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
