package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu

import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode

internal sealed class ContentNode(
  val id: String,
  val title: String,
  val toolTipText: String? = null,
  val icon: Icon? = null,
  val isSecondaryNode: Boolean = false,
) : DefaultMutableTreeNode(title) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}