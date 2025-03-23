package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

@Suppress("UNCHECKED_CAST")
class IntSettingProperty(
  title: String,
  description: String?,
  group: SettingsGroup?,
  settingValue: IntValue,
) :
  SettingProperty<Int, IntValue>(
    title = title,
    description = description,
    group = group,
    settingValue = settingValue,
    initialValue = settingValue.defaultValue,
  ) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun toPersistent(): String? {
    val value = get()
    return if (value != settingValue.defaultValue) {
      value.toString()
    } else {
      null
    }
  }

  override fun fromPersistent(value: String) {
    set(value.toInt())
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
