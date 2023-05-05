@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugins.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.dispatcher.SingleEventDispatcher
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import java.util.concurrent.atomic.AtomicReference

class ValueProperty<T>(initialValue: T) : ObservableMutableProperty<T> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val value = AtomicReference(initialValue)
  private val changeDispatcher = SingleEventDispatcher.create<ChangeEvent<T>>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun afterChange(parentDisposable: Disposable?, listener: (T) -> Unit) =
    changeDispatcher.whenEventHappened(parentDisposable) { listener(it.newValue) }

  fun afterChangeConsumeEvent(parentDisposable: Disposable?, listener: (ChangeEvent<T>) -> Unit) =
    changeDispatcher.whenEventHappened(parentDisposable, listener)

  override fun set(value: T) {
    set(value, null)
  }

  internal fun setWithUncheckedCast(value: Any) {
    @Suppress("UNCHECKED_CAST")
    set(value as T)
  }

  fun set(value: T, changeId: String?, fireEvent: Boolean = true) {
    val oldValue = this.value.get()
    this.value.set(value)
    if (fireEvent) {
      changeDispatcher.fireEvent(ChangeEvent(changeId, oldValue, value))
    }
  }

  override fun get(): T = this.value.get()

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class ChangeEvent<T>(val id: String?, val oldValue: T, val newValue: T) {

    fun valueChanged() = oldValue != newValue
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}