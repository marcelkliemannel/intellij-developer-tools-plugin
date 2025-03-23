package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class InternalSetting(val groupId: String = "")
