package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle.ID
import org.jetbrains.annotations.PropertyKey

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Setting(
  @PropertyKey(resourceBundle = ID) val titleBundleKey: String,
  @PropertyKey(resourceBundle = ID) val descriptionBundleKey: String = "",
  val groupId: String = "",
  val order: Int,
)
