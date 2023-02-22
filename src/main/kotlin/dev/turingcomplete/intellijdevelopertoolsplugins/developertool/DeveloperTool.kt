package dev.turingcomplete.intellijdevelopertoolsplugins.developertool

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolsService
import javax.swing.JComponent
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class DeveloperTool(val id: String, val title: String, val description: String? = null) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var panel: DialogPanel
  private val validationListeners = mutableSetOf<(List<ValidationInfo>) -> Unit>()
  private val propertyChangeListener = mutableSetOf<(String) -> Unit>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun createComponent(project: Project?, parentDisposable: Disposable): JComponent {
    panel = panel {
      buildUi(project, parentDisposable)
    }
    panel.registerValidators(parentDisposable)
    return panel
  }

  abstract fun Panel.buildUi(project: Project?, parentDisposable: Disposable)

  open fun activated() {}

  fun validate(): List<ValidationInfo> {
    val result = panel.validateAll()
    validationListeners.forEach { it(result) }
    return result
  }

  fun <T> createProperty(key: String, defaultValue: T): ReadWriteProperty<Any?, T> {
    val initialValue = getProperty(key, defaultValue)
    return Delegates.observable(initialValue, propertyChanged(key, defaultValue))
  }

  fun registerValidationListeners(listener: (List<ValidationInfo>) -> Unit) {
    validationListeners.add(listener)
  }

  override fun toString(): String = title

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    other as DeveloperTool

    if (id != other.id) {
      return false
    }

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun <T> propertyChanged(key: String, defaultValue: T): (KProperty<*>, T, T) -> Unit = { _, old, new ->
    if (old != new) {
      if (new != defaultValue) {
        setProperty(key, new as Any)
      }
      else {
        unsetProperty(key)
      }
    }
  }

  private fun setProperty(key: String, value: Any) {
    DeveloperToolsService.instance.setProperty(this::class, key, value)
  }

  private fun unsetProperty(key: String) {
    DeveloperToolsService.instance.unsetProperty(this::class, key)
  }

  private fun <T> getProperty(key: String, defaultValue: T): T =
    DeveloperToolsService.instance.getProperty(this::class, key, defaultValue)


  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}