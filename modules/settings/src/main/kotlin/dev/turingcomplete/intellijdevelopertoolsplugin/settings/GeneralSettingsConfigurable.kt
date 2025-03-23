package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import dev.turingcomplete.intellijdevelopertoolsplugin.common.not
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.generalSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.Companion.ACTION_HANDLING_GROUP_ID
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.AnySettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsConfigurable
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle

class GeneralSettingsConfigurable : SettingsConfigurable<GeneralSettings>(
  settings = generalSettings
) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getDisplayName(): @NlsContexts.ConfigurableName String? =
    SettingsBundle.message("general-settings.title")

  override fun buildGroupSettingsUi(panel: Panel, group: SettingsGroup, groupSettings: List<AnySettingProperty>) {
    when (group.id) {
      ACTION_HANDLING_GROUP_ID -> {
        with(panel) {
          buttonsGroup {
            row {
              radioButton(SettingsBundle.message("general-settings.action-handling.auto-detect"))
                .bindSelected(settings.autoDetectActionHandlingInstance)

              radioButton(SettingsBundle.message("general-settings.action-handling.selected"))
                .bindSelected(settings.autoDetectActionHandlingInstance.not())
                .gap(RightGap.SMALL)
              comboBox(GeneralSettings.ActionHandlingInstance.entries)
                .bindItem(settings.selectedActionHandlingInstance)
                .enabledIf(settings.autoDetectActionHandlingInstance.not())
            }
          }
        }
      }

      else -> super.buildGroupSettingsUi(panel, group, groupSettings)
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
