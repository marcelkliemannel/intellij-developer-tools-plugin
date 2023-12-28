package dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.content

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.settings.DeveloperToolsInstanceSettings
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.menu.ContentNode
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.menu.DeveloperToolNode
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.menu.GroupNode
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.menu.ToolsMenuTree
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty
import javax.swing.JPanel

internal open class ContentPanelHandler(
  project: Project?,
  protected val parentDisposable: Disposable,
  settings: DeveloperToolsInstanceSettings,
  groupNodeSelectionEnabled: Boolean = true
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  protected var selectedContentNode = ValueProperty<ContentNode?>(null)

  val contentPanel = BorderLayoutPanel()
  val toolsMenuTree: ToolsMenuTree

  private val cachedGroupsPanels = mutableMapOf<String, GroupContentPanel>()
  private val cachedDeveloperToolsPanels = mutableMapOf<DeveloperToolNode, DeveloperToolContentPanel>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    // The creation of `ToolsMenuTree` will trigger the initial node selection
    toolsMenuTree = ToolsMenuTree(project, parentDisposable, settings, groupNodeSelectionEnabled) {
      handleContentNodeSelection(it)
    }
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  protected open fun createDeveloperToolContentPanel(developerToolNode: DeveloperToolNode): DeveloperToolContentPanel =
    DeveloperToolContentPanel(developerToolNode)

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun handleContentNodeSelection(new: ContentNode?) {
    val old = selectedContentNode.get()
    if (old != new) {
      if (old is DeveloperToolNode) {
        cachedDeveloperToolsPanels[old]?.deselected()
      }

      if (new == null) {
        return
      }

      when (new) {
        is GroupNode -> {
          setContentPanel(cachedGroupsPanels.getOrPut(new.developerToolGroup.id) {
            GroupContentPanel(new) {
              selectedContentNode.set(it)
              TreeUtil.selectNode(toolsMenuTree, it)
            }
          }.panel)
          selectedContentNode.set(new)
        }

        is DeveloperToolNode -> {
          setContentPanel(cachedDeveloperToolsPanels.getOrPut(new) { createDeveloperToolContentPanel(new) }.also { it.selected() })
          selectedContentNode.set(new)
        }

        else -> error("Unexpected menu node: ${new::class}")
      }
    }
  }

  private fun setContentPanel(nodePanel: JPanel) {
    contentPanel.apply {
      removeAll()
      addToCenter(nodePanel)
      revalidate()
      repaint()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}