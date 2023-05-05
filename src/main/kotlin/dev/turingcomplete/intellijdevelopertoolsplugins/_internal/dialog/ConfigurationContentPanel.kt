package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService

class ConfigurationContentPanel {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val panel: DialogPanel = panel {
    row {
      checkBox("Remember configuration")
        .bindSelected(DeveloperToolsPluginService.instance.saveConfiguration)
    }
    row {
      checkBox("Remember inputs")
        .bindSelected(DeveloperToolsPluginService.instance.saveInputs)
        .comment("Secrets will not be stored.")
    }
    row {
      checkBox("Load examples")
        .bindSelected(DeveloperToolsPluginService.instance.loadExamples)
    }
  }.apply { border = JBEmptyBorder(0, 8, 0, 8) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}