package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.ValidationInfoBuilder
import kotlin.properties.Delegates

class ErrorHolder {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val setChangedListeners = mutableListOf<(String?) -> Unit>()

  private var error: String? by Delegates.observable(null) { _, oldValue, newValue ->
    if (oldValue != newValue) {
      setChangedListeners.forEach { it(newValue) }
    }
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun set(exception: Exception) {
    error = exception.message ?: exception::class.java.simpleName
  }

  fun set(message: String) {
    error = message
  }

  fun unset() {
    error = null
  }

  fun asComponentPredicate() = object : ComponentPredicate() {

    override fun addListener(listener: (Boolean) -> Unit) {
      setChangedListeners.add { listener(error != null) }
    }

    override fun invoke(): Boolean = error != null
  }

  fun <T> asValidation(): ValidationInfoBuilder.(T) -> ValidationInfo? =
    { error?.let { ValidationInfo("<html>$it</html>") } }

  /**
   * Creates a [ObservableProperty] that will return an empty string if there is
   * no error.
   */
  fun asObservableNonNullProperty(): ObservableProperty<String> = object : ObservableProperty<String> {

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