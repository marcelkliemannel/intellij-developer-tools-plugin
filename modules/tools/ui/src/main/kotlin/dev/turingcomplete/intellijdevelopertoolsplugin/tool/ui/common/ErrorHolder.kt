package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.ValidationInfoBuilder
import javax.swing.JComponent

class ErrorHolder(
  // Icon must be used in a `text()` cell, a `label()` cell will not work.
  private val addErrorIconToMessage: Boolean = false,
  private val surroundMessageWithHtml: Boolean = true,
) {
  // -- Properties ---------------------------------------------------------- //

  private val changeListeners = mutableListOf<(List<String>) -> Unit>()

  private var errors = mutableListOf<String>()

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun add(prefix: String, error: Throwable) {
    errors.add("$prefix ${error.message ?: error::class.java.simpleName}")
    fireChange()
  }

  fun add(error: Throwable) {
    errors.add(error.message ?: error::class.java.simpleName)
    fireChange()
  }

  fun add(message: String) {
    errors.add(message)
    fireChange()
  }

  fun addIfEmpty(message: String) {
    if (errors.isNotEmpty()) {
      return
    }
    errors.add(message)
    fireChange()
  }

  fun clear() {
    errors.clear()
    fireChange()
  }

  fun isSet() = errors.isNotEmpty()

  fun isNotSet() = errors.isEmpty()

  fun asComponentPredicate() =
    object : ComponentPredicate() {

      override fun addListener(listener: (Boolean) -> Unit) {
        changeListeners.add { listener(it.isNotEmpty()) }
      }

      override fun invoke(): Boolean = errors.isNotEmpty()
    }

  fun <T> asValidation(
    forComponent: JComponent? = null
  ): ValidationInfoBuilder.(T) -> ValidationInfo? {
    return { formatErrors()?.let { ValidationInfo(it, forComponent) } }
  }

  /**
   * Creates a [ObservableMutableProperty] that will return an empty string if there is no error.
   *
   * The `set()` operation is not supported, but a [com.intellij.ui.dsl.builder.Row.text] requires
   * [ObservableMutableProperty] and not only a
   * [com.intellij.openapi.observable.properties.ObservableProperty].
   */
  fun asPropertyForTextCell(): ObservableMutableProperty<String> =
    object : ObservableMutableProperty<String> {

      override fun afterChange(listener: (String) -> Unit) {
        changeListeners.add { listener(formatErrors() ?: "") }
      }

      override fun get(): String = formatErrors() ?: ""

      override fun set(value: String) {
        throw UnsupportedOperationException()
      }
    }

  // -- Private Methods ----------------------------------------------------- //

  private fun formatErrors(): String? =
    errors.let {
      when (it.size) {
        0 -> null
        1 ->
          if (surroundMessageWithHtml) "<html>${formatError(it[0])}</html>" else formatError(it[0])
        else ->
          it.joinToString(
            separator = "\n",
            prefix =
              "${if (surroundMessageWithHtml) "<html>" else ""}<ul style='padding: 0; margin: 0'>",
            postfix = "</ul>${if (surroundMessageWithHtml) "</html>" else ""}",
          ) { error ->
            "<li>${formatError(error)}</li>"
          }
      }
    }

  private fun formatError(message: String) =
    "${if (addErrorIconToMessage) "<icon src='AllIcons.General.Error'>&nbsp;" else ""}$message"

  private fun fireChange() {
    changeListeners.forEach { it(errors) }
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
