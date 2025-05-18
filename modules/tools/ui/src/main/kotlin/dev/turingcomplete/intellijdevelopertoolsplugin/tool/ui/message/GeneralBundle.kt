package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message

import com.intellij.DynamicBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.GeneralBundle.ID
import org.jetbrains.annotations.PropertyKey

object GeneralBundle : DynamicBundle(GeneralBundle::class.java, ID) {
  // -- Properties ---------------------------------------------------------- //

  private const val ID = "message.GeneralBundle"

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = ID) key: String, vararg params: Any): String =
    getMessage(key, *params)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
