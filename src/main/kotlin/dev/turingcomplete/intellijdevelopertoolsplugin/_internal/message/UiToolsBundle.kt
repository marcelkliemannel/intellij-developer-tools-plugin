package dev.turingcomplete.intellijdevelopertoolsplugin._internal.message

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

object UiToolsBundle {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private const val ID = "messages.UiToolsBundle"

  private val instance: DynamicBundle = DynamicBundle(UiToolsBundle::class.java, ID)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = ID) key: String, vararg params: Any): String =
    instance.getMessage(key, *params)

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}