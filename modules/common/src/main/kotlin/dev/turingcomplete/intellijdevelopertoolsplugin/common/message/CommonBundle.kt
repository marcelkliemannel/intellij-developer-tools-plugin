package dev.turingcomplete.intellijdevelopertoolsplugin.common.message

import com.intellij.DynamicBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.common.message.CommonBundle.ID
import org.jetbrains.annotations.PropertyKey

object CommonBundle : DynamicBundle(CommonBundle::class.java, ID) {
  // -- Properties ---------------------------------------------------------- //

  private const val ID = "message.CommonBundle"

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = ID) key: String, vararg params: Any): String =
    getMessage(key, *params)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
