package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

@SettingValue
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class BooleanValue(val defaultValue: Boolean)
