package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu

import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolGroup

class GroupNode(val developerUiToolGroup: DeveloperUiToolGroup) :
  ContentNode(
    id = developerUiToolGroup.id,
    title = developerUiToolGroup.menuTitle
  ) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
