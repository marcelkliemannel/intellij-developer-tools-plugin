package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle.ID
import org.jetbrains.annotations.PropertyKey

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class SettingsGroup(
  val id: String,
  @PropertyKey(resourceBundle = ID) val titleBundleKey: String,
  @PropertyKey(resourceBundle = ID) val descriptionBundleKey: String = "",
  val order: Int,
) {

  companion object {

    fun SettingsGroup.isDefaultGroup(): Boolean = this == defaultSettingsGroup

    val defaultSettingsGroup =
      SettingsGroup(id = "defaultGroup", titleBundleKey = "", descriptionBundleKey = "", order = -1)
  }
}
