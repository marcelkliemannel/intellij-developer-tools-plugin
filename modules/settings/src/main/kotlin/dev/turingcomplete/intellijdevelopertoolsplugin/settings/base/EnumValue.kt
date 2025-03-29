package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import kotlin.reflect.KClass

@SettingValue
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnumValue<T : Enum<T>>(
  val enumClass: KClass<T>,
  val defaultValueName: String,
  val displayTextProvider: KClass<out DisplayTextProvider<T>>,
) {

  interface DisplayTextProvider<T> {

    fun toDisplayText(value: T): String

    @Suppress("UNCHECKED_CAST")
    fun toDisplayTextUncheckedCast(value: Any): String = toDisplayText(value as T)
  }
}
