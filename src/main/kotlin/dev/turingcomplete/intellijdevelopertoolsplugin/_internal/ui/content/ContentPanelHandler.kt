package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.content

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsInstanceSettings
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.AddOpenMainDialogActionToMainToolbarTask
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.ContentNode
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.DeveloperToolNode
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.GroupNode
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.ToolsMenuTree
import javax.swing.JPanel

internal open class ContentPanelHandler(
  project: Project?,
  protected val parentDisposable: Disposable,
  settings: DeveloperToolsInstanceSettings,
  groupNodeSelectionEnabled: Boolean = true,
  promoteMainDialog: Boolean = false,
  prioritizeVerticalLayout: Boolean = false
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  protected var selectedContentNode = ValueProperty<ContentNode?>(null)

  val contentPanel = BorderLayoutPanel()
  val toolsMenuTree: ToolsMenuTree

  private val innerContentPanel = BorderLayoutPanel()
  private val mainDialogPromotionPanel = BorderLayoutPanel()

  private val cachedGroupsPanels = mutableMapOf<String, GroupContentPanel>()
  private val cachedDeveloperToolsPanels = mutableMapOf<DeveloperToolNode, DeveloperToolContentPanel>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    // The creation of `ToolsMenuTree` will trigger the initial node selection
    toolsMenuTree = ToolsMenuTree(project, parentDisposable, settings, groupNodeSelectionEnabled, prioritizeVerticalLayout) {
      handleContentNodeSelection(it)
    }

    initMainDialogPromotionPanel(promoteMainDialog)

    contentPanel.apply {
      addToTop(mainDialogPromotionPanel)
      addToCenter(innerContentPanel)
    }
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  protected open fun createDeveloperToolContentPanel(developerToolNode: DeveloperToolNode): DeveloperToolContentPanel =
    DeveloperToolContentPanel(developerToolNode)

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun initMainDialogPromotionPanel(promoteMainDialog: Boolean) {
    if (!promoteMainDialog) {
      return
    }

    if (!DeveloperToolsApplicationSettings.instance.promoteAddOpenMainDialogActionToMainToolbar) {
      return
    }

    val addOpenMainDialogActionToMainToolbarTask = AddOpenMainDialogActionToMainToolbarTask.createIfAvailable()
      ?: return

    val promoteMainDialogNotificationPanel = PromoteMainDialogNotificationPanel { addOpenMainDialogActionToMainToolbar ->
      DeveloperToolsApplicationSettings.instance.apply {
        this@apply.addOpenMainDialogActionToMainToolbar = addOpenMainDialogActionToMainToolbar
        promoteAddOpenMainDialogActionToMainToolbar = false
      }

      if (addOpenMainDialogActionToMainToolbar) {
        addOpenMainDialogActionToMainToolbarTask.run()
      }

      mainDialogPromotionPanel.isVisible = false
    }
    mainDialogPromotionPanel.addToCenter(promoteMainDialogNotificationPanel)
  }

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
          setContentPanel(cachedGroupsPanels.getOrPut(new.developerUiToolGroup.id) {
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
    innerContentPanel.apply {
      removeAll()
      addToCenter(nodePanel)
      revalidate()
      repaint()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class PromoteMainDialogNotificationPanel(
    closeNotification: (Boolean) -> Unit
  ) : EditorNotificationPanel(Status.Promo) {

    init {
      text = "<html>The tools are also available as a standalone window.</html>"
      myLinksPanel.add(ActionLink("Add to main toolbar") { closeNotification(true) })
      setCloseAction { closeNotification(false) }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}