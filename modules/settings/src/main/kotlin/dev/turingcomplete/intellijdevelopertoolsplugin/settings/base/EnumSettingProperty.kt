package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import dev.turingcomplete.intellijdevelopertoolsplugin.common.findEnumValueByName
import dev.turingcomplete.intellijdevelopertoolsplugin.common.getEnumValueByNameOrThrow

class EnumSettingProperty<T : Enum<T>>(
  descriptor: Descriptor?,
  group: SettingsGroup,
  settingValue: EnumValue<T>,
) :
  SettingProperty<T, EnumValue<T>>(
    descriptor = descriptor,
    group = group,
    settingValue = settingValue,
    initialValue = getDefaultValue(settingValue),
  ) {
  // -- Properties ---------------------------------------------------------- //

  private val defaultValue = getDefaultValue(settingValue)

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun toPersistent(): String? {
    val value = get()
    return if (value != defaultValue) {
      value.name
    } else {
      null
    }
  }

  override fun fromPersistent(value: String) {
    settingValue.enumClass.findEnumValueByName(value)?.let { set(it) }
  }

  fun getAllEnumValues(): List<T> = settingValue.enumClass.java.enumConstants.toList()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private fun <T : Enum<T>> getDefaultValue(settingValue: EnumValue<T>): T =
      settingValue.enumClass.getEnumValueByNameOrThrow(settingValue.defaultValueName)
  }
}
