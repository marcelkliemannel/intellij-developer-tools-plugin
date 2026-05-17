package dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.message

import com.intellij.DynamicBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.message.KotlinDependentBundle.ID
import org.jetbrains.annotations.PropertyKey

object KotlinDependentBundle : DynamicBundle(KotlinDependentBundle::class.java, ID) {
  // -- Properties ---------------------------------------------------------- //

  private const val ID = "message.KotlinDependentBundle"

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = ID) key: String, vararg params: Any): String =
    getMessage(key, *params)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
