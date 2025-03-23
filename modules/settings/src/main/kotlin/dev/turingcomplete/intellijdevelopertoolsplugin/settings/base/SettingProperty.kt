package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty

@Suppress("UNCHECKED_CAST")
sealed class SettingProperty<T: Any, U: Annotation>(
  val title: String,
  val description: String?,
  val group: SettingsGroup?,
  val settingValue: U,
  val initialValue: T
) : ValueProperty<T>(initialValue) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  abstract fun toPersistent(): String?

  abstract fun fromPersistent(value: String)

  fun isModified(): Boolean = initialValue != get()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}

typealias AnySettingProperty = SettingProperty<Any, Annotation>
