package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message

import com.intellij.DynamicBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle.ID
import org.jetbrains.annotations.PropertyKey

object EditorToolsBundle : DynamicBundle(EditorToolsBundle::class.java, ID) {
  // -- Properties ---------------------------------------------------------- //

  private const val ID = "message.EditorToolsBundle"

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun message(@PropertyKey(resourceBundle = ID) key: String, vararg params: Any): String =
    getMessage(key, *params)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
