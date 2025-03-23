package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import kotlin.reflect.KClass

@SettingValue
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnumValue<T : Enum<T>>(
  val enumClass: KClass<T>,
  val defaultValueName: String
)
