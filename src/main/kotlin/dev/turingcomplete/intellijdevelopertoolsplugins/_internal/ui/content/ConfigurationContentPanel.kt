package dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.content

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.UiUtils.createLink

class ConfigurationContentPanel {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val panel: DialogPanel = panel {
    row {
      label("Configuration")
        .applyToComponent { font = JBFont.label().asBold() }
        .align(Align.FILL)
        .resizableColumn()

      link("Reset") {
        DeveloperToolsPluginService.saveConfiguration = DeveloperToolsPluginService.SAVE_CONFIGURATION_DEFAULT
        DeveloperToolsPluginService.saveInputs = DeveloperToolsPluginService.SAVE_INPUTS_DEFAULT
        DeveloperToolsPluginService.saveSecrets = DeveloperToolsPluginService.SAVE_SECRETS_DEFAULT
        DeveloperToolsPluginService.loadExamples = DeveloperToolsPluginService.LOAD_EXAMPLES_DEFAULT
        DeveloperToolsPluginService.loadExamples = DeveloperToolsPluginService.LOAD_EXAMPLES_DEFAULT
        DeveloperToolsPluginService.loadExamples = DeveloperToolsPluginService.LOAD_EXAMPLES_DEFAULT
        DeveloperToolsPluginService.loadExamples = DeveloperToolsPluginService.LOAD_EXAMPLES_DEFAULT
      }
    }

    indent {
      row {
        checkBox("Remember configuration")
          .bindSelected(DeveloperToolsPluginService.instance.saveConfiguration)
      }
      row {
        checkBox("Remember inputs")
          .bindSelected(DeveloperToolsPluginService.instance.saveInputs)
      }
      row {
        checkBox("Remember secrets")
          .bindSelected(DeveloperToolsPluginService.instance.saveSecrets)
      }.comment("Secrets are stored in the <a href='https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html#storage'>system keychain</a>.") {
        BrowserUtil.browse(it.url)
      }
      row {
        checkBox("Load examples")
          .bindSelected(DeveloperToolsPluginService.instance.loadExamples)
      }.comment("Changes will take effect the next time the dialog is opened.")

      groupRowsRange("Default Editor Settings") {
        row {
          checkBox("Soft-wrap")
            .bindSelected(DeveloperToolsPluginService.instance.editorSoftWraps)
        }
        row {
          checkBox("Show special characters")
            .bindSelected(DeveloperToolsPluginService.instance.editorShowSpecialCharacters)
        }
        row {
          checkBox("Show whitespaces")
            .bindSelected(DeveloperToolsPluginService.instance.editorShowWhitespaces)
        }
        row {
          comment("Some changes will take effect the next time the dialog is opened.")
        }
      }

      groupRowsRange("Advanced") {
        row {
          checkBox("Dialog is modal")
            .bindSelected(DeveloperToolsPluginService.instance.dialogIsModal)
        }.comment("Changes will take effect the next time the dialog is opened.")
      }

      row {
        cell(
          createLink(
            title = "Make a feature request or report an issue",
            url = "https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues"
          )
        )
      }.topGap(TopGap.MEDIUM)
    }
  }.apply { border = JBEmptyBorder(0, 8, 0, 8) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}