package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.ui.layout.ComponentPredicate
import kotlin.properties.Delegates

class ErrorHolder {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val setChangedListeners = mutableListOf<(String?) -> Unit>()

  var error: String? by Delegates.observable(null) { _, oldValue, newValue ->
    if (oldValue != newValue) {
      setChangedListeners.forEach { it(newValue) }
    }
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun asComponentPredicate() = object: ComponentPredicate() {

    override fun addListener(listener: (Boolean) -> Unit) {
      setChangedListeners.add { listener(error != null) }
    }

    override fun invoke(): Boolean = error != null
  }

  /**
   * Creates a [ObservableProperty] that will return an empty string if there is
   * no error.
   */
  fun asObservableNonNullProperty(): ObservableProperty<String> = object: ObservableProperty<String> {

    override fun afterChange(listener: (String) -> Unit) {
      setChangedListeners.add { listener(it ?: "") }
    }

    override fun afterChange(listener: (String) -> Unit, parentDisposable: Disposable) {
      setChangedListeners.add { listener(it ?: "") }
    }

    override fun get(): String = error ?: ""
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}