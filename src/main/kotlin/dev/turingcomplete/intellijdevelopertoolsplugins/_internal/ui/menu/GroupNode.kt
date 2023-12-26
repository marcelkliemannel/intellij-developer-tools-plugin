package dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.menu

import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolGroup

internal class GroupNode(val developerToolGroup: DeveloperToolGroup) :
  ContentNode(
    id = developerToolGroup.id,
    title = developerToolGroup.menuTitle
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}