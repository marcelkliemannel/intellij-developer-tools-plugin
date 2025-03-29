package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle.SETTINGS_BUNDLE_ID
import org.jetbrains.annotations.PropertyKey

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Setting(
  @PropertyKey(resourceBundle = SETTINGS_BUNDLE_ID) val titleBundleKey: String,
  @PropertyKey(resourceBundle = SETTINGS_BUNDLE_ID) val descriptionBundleKey: String = "",
  val groupId: String = "",
  val order: Int,
)
