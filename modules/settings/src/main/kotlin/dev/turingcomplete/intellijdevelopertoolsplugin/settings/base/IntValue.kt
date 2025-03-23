package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

@SettingValue
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntValue(
  val defaultValue: Int,
  val min: Int = Int.MIN_VALUE,
  val max: Int = Int.MAX_VALUE,
)
