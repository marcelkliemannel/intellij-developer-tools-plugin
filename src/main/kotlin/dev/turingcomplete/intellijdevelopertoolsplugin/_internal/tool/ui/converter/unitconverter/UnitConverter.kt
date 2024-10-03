package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ComponentUtil.findComponentsOfType
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

abstract class UnitConverter(
  private val parentDisposable: Disposable,
  val title: String
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var component: DialogPanel

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun createComponent(): JComponent {
    component = panel {
      buildUi()
      buildSettingsUi()
    }

    findComponentsOfType(component, DialogPanel::class.java).forEach {
      it.registerValidators(parentDisposable)
    }

    return component
  }

  protected abstract fun Panel.buildUi()

  open fun Panel.buildSettingsUi() {
    // Override if needed
  }

  fun validate(): List<ValidationInfo> =
    findComponentsOfType(component, DialogPanel::class.java).flatMap { it.validateAll() }.toList()

  open fun sync() {
    // Override if needed
  }

  open fun activate() {
    // Override if needed
  }

  open fun deactivate() {
    // Override if needed
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}