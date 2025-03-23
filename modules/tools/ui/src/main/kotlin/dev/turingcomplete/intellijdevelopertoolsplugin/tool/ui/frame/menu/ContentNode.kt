package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu

import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode

sealed class ContentNode(
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
