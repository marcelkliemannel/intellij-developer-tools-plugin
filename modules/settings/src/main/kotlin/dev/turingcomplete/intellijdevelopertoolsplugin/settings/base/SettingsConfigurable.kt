package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import com.intellij.openapi.options.Configurable
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.generator.nova.GenerationSpec.Companion.nullIfEmpty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsGroup.Companion.isDefaultGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsHandler.settingsContainer
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle
import javax.swing.JComponent
import kotlin.reflect.full.createInstance

abstract class SettingsConfigurable<T : Settings>(protected val settings: T) : Configurable {
  // -- Properties ---------------------------------------------------------- //

  private val derivatedSettingsContainer = settings.settingsContainer().derivate()

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  final override fun createComponent(): JComponent? = panel {
    derivatedSettingsContainer.settingProperties
      .asSequence()
      .filter { it.value.descriptor != null }
      .groupBy({ it.value.group }, { it.value })
      .toSortedMap { firstGroup, secondGroup ->
        (firstGroup?.order ?: 0).compareTo(secondGroup?.order ?: 0)
      }
      .forEach { group, settings ->
        val sortedSettings = settings.sortedBy { it.descriptor!!.order }
        if (group.isDefaultGroup()) {
          sortedSettings.forEach { buildSettingUi(it) }
        } else {
          group(SettingsBundle.message(group.titleBundleKey)) {
            group.descriptionBundleKey.nullIfEmpty()?.let { row { comment(it) } }
            buildGroupSettingsUi(this, group, sortedSettings)
          }
        }
      }
  }

  protected open fun buildGroupSettingsUi(
    panel: Panel,
    group: SettingsGroup,
    groupSettings: List<AnySettingProperty>,
  ) {
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
    val descriptor = settingProperty.descriptor as SettingProperty.Descriptor
    row {
        when (settingProperty) {
          is BooleanSettingProperty -> {
            val checkBox = checkBox(descriptor.title).bindSelected(settingProperty)
            descriptor.description?.let { checkBox.comment(it) }
          }

          is IntSettingProperty -> {
            val intRange =
              IntRange(
                start = settingProperty.settingValue.min,
                endInclusive = settingProperty.settingValue.max,
              )
            val intTextField = intTextField(intRange).bindIntText(settingProperty)
            intTextField.label(descriptor.title)
            descriptor.description?.let { intTextField.comment(it) }
          }

          is EnumSettingProperty<*> -> {
            val enumValueRenderer =
              SimpleListCellRenderer.create<Any> { label, value, _ ->
                label.text =
                  settingProperty.settingValue.displayTextProvider
                    .createInstance()
                    .toDisplayTextUncheckedCast(value)
              }
            val comboBox =
              comboBox(
                  settingProperty.getAllEnumValues().uncheckedCastTo<List<Any>>(),
                  enumValueRenderer,
                )
                .bindItem(settingProperty)
                .label(descriptor.title)
            descriptor.description?.let { comboBox.comment(it) }
          }
        }
      }
      .layout(RowLayout.PARENT_GRID)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
