package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

object GeneralBundle {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private const val ID = "message.GeneralBundle"

  private val instance: DynamicBundle = DynamicBundle(GeneralBundle::class.java, ID)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = ID) key: String, vararg params: Any): String =
    instance.getMessage(key, *params)

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
