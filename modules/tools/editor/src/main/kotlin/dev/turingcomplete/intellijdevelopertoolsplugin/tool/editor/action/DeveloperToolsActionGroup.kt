package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.action

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.IconManager
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.message.EditorToolsBundle

class DeveloperToolsActionGroup :
  DefaultActionGroup(EditorToolsBundle.message("developer-tools-action-group.title"), true) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //

  init {
    templatePresentation.icon = icon
    templatePresentation.isHideGroupIfEmpty = true
  }

  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val icon =
      IconManager.getInstance()
        .getIcon(
          "dev/turingcomplete/intellijdevelopertoolsplugin/icons/action.svg",
          DeveloperToolsActionGroup::class.java.classLoader,
        )
  }
}
