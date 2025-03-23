package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.generator.nova.GenerationSpec.Companion.nullIfEmpty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsHandler.settingsContainer
import javax.swing.JComponent

abstract class SettingsConfigurable<T: Settings>(
  protected val settings: T,
) : Configurable {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val derivatedSettingsContainer = settings.settingsContainer().derivate()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  
  final override fun createComponent(): JComponent? = panel {
    derivatedSettingsContainer.settingProperties
      .asSequence()
      .groupBy({ it.value.group }, { it.value })
      .forEach { group, settings ->
        if (group != null) {
          group(group.titleBundleKey) {
            group.descriptionBundleKey.nullIfEmpty()?.let {
              row {
                comment(it)
              }
            }
            buildGroupSettingsUi(this, group, settings)
          }
        }
        else {
          settings.forEach { buildSettingUi(it) }
        }
      }
  }

  protected open fun buildGroupSettingsUi(panel: Panel, group: SettingsGroup, groupSettings: List<AnySettingProperty>) {
    groupSettings.forEach { panel.buildSettingUi(it) }
  }

  final override fun isModified(): Boolean = derivatedSettingsContainer.isModified()

  final override fun reset() {
    derivatedSettingsContainer.reset()
  }

  final override fun apply() {
    derivatedSettingsContainer.apply()
  }

  protected fun Panel.buildSettingUi(settingProperty: AnySettingProperty) {
    row {
      when (settingProperty) {
        is BooleanSettingProperty -> {
          val checkBox = checkBox(settingProperty.title).bindSelected(settingProperty)
          settingProperty.description?.let { checkBox.comment(it) }
        }

        is IntSettingProperty -> {
          val intRange = IntRange(start = settingProperty.settingValue.min, endInclusive = settingProperty.settingValue.max)
          val intTextField = intTextField(intRange).bindIntText(settingProperty)
          intTextField.label(settingProperty.title)
          settingProperty.description?.let { intTextField.comment(it) }
        }

        is EnumSettingProperty<*> -> {
          val comboBox = comboBox(settingProperty.getAllEnumValues())
            .bindItem(settingProperty.uncheckedCastTo<MutableProperty<Enum<*>?>>())
            .label(settingProperty.title)
          settingProperty.description?.let { comboBox.comment(it) }
        }
      }
    }.layout(RowLayout.PARENT_GRID)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
