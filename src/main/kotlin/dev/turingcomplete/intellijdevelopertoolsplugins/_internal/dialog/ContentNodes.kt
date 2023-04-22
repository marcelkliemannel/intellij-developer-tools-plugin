package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolGroup
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //
// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

abstract class ContentNode(val id: String, title: String, private val weight: Int) : DefaultMutableTreeNode(title) {

  open fun selected() {
    // Override if needed
  }

  open fun deselected() {
    // Override if needed
  }

  fun sortChildren() {
    doSortChildren(children)
  }

  private fun doSortChildren(children: Vector<TreeNode>?) {
    children ?: return

    children.sortWith { o1, o2 -> (o1 as ContentNode).weight.compareTo((o2 as ContentNode).weight) }
    children.forEach { (it as ContentNode).sortChildren() }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class RootNode : ContentNode("root", "Root", Int.MIN_VALUE)

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class GroupNode(val developerToolGroup: DeveloperToolGroup) :
  ContentNode(
          id = developerToolGroup.id,
          title = developerToolGroup.menuTitle,
          weight = checkNotNull(developerToolGroup.weight) { "No weight set" }
  )

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class DeveloperToolNode(
  developerToolId: String,
  parentDisposable: Disposable,
  val developerToolContext: DeveloperToolContext,
  private val developerToolCreator: () -> DeveloperTool,
  weight: Int
) : ContentNode(developerToolId, developerToolContext.menuTitle, weight) {

  val developerTool by lazy {
    val developerTool = developerToolCreator()
    Disposer.register(parentDisposable, developerTool)
    developerTool
  }

  override fun selected() {
    developerTool.activated()
  }

  override fun deselected() {
    developerTool.deactivated()
  }
}
