package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog.structure

import java.util.*
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

internal sealed class ContentNode(
  val id: String,
  val title: String,
  val toolTipText: String? = null,
  private val weight: Int,
  val icon: Icon? = null,
  val isSecondaryNode: Boolean = false,
) : DefaultMutableTreeNode(title) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun sortChildren() {
    doSortChildren(children)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun doSortChildren(children: Vector<TreeNode>?) {
    children ?: return

    children.sortWith { o1, o2 -> (o1 as ContentNode).weight.compareTo((o2 as ContentNode).weight) }
    children.forEach { (it as ContentNode).sortChildren() }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}