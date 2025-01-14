package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu

import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolGroup

internal class GroupNode(val developerUiToolGroup: DeveloperUiToolGroup) :
  ContentNode(
    id = developerUiToolGroup.id,
    title = developerUiToolGroup.menuTitle
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}