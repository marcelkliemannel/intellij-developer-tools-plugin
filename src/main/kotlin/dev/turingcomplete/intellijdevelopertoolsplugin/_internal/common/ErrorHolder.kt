package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.ValidationInfoBuilder
import javax.swing.JComponent

class ErrorHolder {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val changeListeners = mutableListOf<(List<String>) -> Unit>()

  private var errors = mutableListOf<String>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun add(postFix: String, exception: Exception) {
    errors.add("$postFix ${exception.message ?: exception::class.java.simpleName}")
    fireChange()
  }

  fun add(exception: Exception) {
    errors.add(exception.message ?: exception::class.java.simpleName)
    fireChange()
  }

  fun add(message: String) {
    errors.add(message)
    fireChange()
  }

  fun clear() {
    errors.clear()
    fireChange()
  }

  fun isSet() = errors.isNotEmpty()

  fun isNotSet() = errors.isEmpty()

  fun asComponentPredicate() = object : ComponentPredicate() {

    override fun addListener(listener: (Boolean) -> Unit) {
      changeListeners.add { listener(it.isNotEmpty()) }
    }

    override fun invoke(): Boolean = errors.isNotEmpty()
  }

  fun <T> asValidation(forComponent: JComponent? = null): ValidationInfoBuilder.(T) -> ValidationInfo? =
    { formatErrors()?.let { ValidationInfo(it, forComponent) } }

  /**
   * Creates a [ObservableProperty] that will return an empty string if there is
   * no error.
   */
  fun asObservableNonNullProperty(): ObservableProperty<String> = object : ObservableProperty<String> {

    override fun afterChange(listener: (String) -> Unit) {
      changeListeners.add { listener(formatErrors() ?: "") }
    }

    override fun get(): String = formatErrors() ?: ""
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun formatErrors(): String? = errors.let {
    when (it.size) {
      0 -> null
      1 -> "<html>${it[0]}</html>"
      else -> it.joinToString(separator = "\n", prefix = "<html><ul>", postfix = "</ul></html>") {
        error -> "<li>$error</li>"
      }
    }
  }

  private fun fireChange() {
    changeListeners.forEach { it(errors) }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}