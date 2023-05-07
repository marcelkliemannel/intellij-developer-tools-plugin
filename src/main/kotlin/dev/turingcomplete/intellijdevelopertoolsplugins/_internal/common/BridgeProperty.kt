package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableMutableProperty

class BridgeProperty<S, T>(
  private val property: ObservableMutableProperty<S>,
  private val setValue: (T) -> S = { throw UnsupportedOperationException() },
  private val getValue: (S) -> T
) : ObservableMutableProperty<T> {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun afterChange(parentDisposable: Disposable?, listener: (T) -> Unit) {
    property.afterChange(parentDisposable) { listener(getValue(it)) }
  }

  override fun set(value: T) {
    property.set(setValue(value))
  }

  override fun get(): T = getValue(property.get())

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}