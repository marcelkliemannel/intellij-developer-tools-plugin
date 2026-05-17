package dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.message

import com.intellij.DynamicBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.message.JavaDependentBundle.ID
import org.jetbrains.annotations.PropertyKey

object JavaDependentBundle : DynamicBundle(JavaDependentBundle::class.java, ID) {
  // -- Properties ---------------------------------------------------------- //

  private const val ID = "message.JavaDependentBundle"

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = ID) key: String, vararg params: Any): String =
    getMessage(key, *params)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
