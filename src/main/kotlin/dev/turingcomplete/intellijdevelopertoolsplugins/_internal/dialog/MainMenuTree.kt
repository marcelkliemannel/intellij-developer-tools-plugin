package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog

import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.RelativeFont
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.render.RenderingUtil
import com.intellij.ui.tree.TreeVisitor.Action.CONTINUE
import com.intellij.ui.tree.TreeVisitor.Action.INTERRUPT
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolGroup
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolFactoryEp
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService.Companion.lastSelectedContentNodeId
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.safeCastTo
import javax.swing.JTree
import javax.swing.event.TreeSelectionListener
import javax.swing.plaf.TreeUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class MainMenuTree(
  private val onContentNodeSelection: (ContentNode) -> Unit,
  project: Project?,
  parentDisposable: Disposable
) : SimpleTree() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val (rootNode, groupNodesToExpand, preferredSelectedDeveloperToolNode) = createTreeNodes(project, parentDisposable)

    model = DefaultTreeModel(rootNode)
    setCellRenderer(MenuTreeNodeRenderer(rootNode))
    putClientProperty(RenderingUtil.ALWAYS_PAINT_SELECTION_AS_FOCUSED, true)
    background = UIUtil.SIDE_PANEL_BACKGROUND
    inputMap.clear()
    TreeUtil.installActions(this)
    isOpaque = true
    selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    addTreeSelectionListener(handleMenuTreeSelection())
    isRootVisible = false
    setExpandableItemsEnabled(false)
    RelativeFont.BOLD.install<SimpleTree>(this)

    groupNodesToExpand.forEach { TreeUtil.promiseExpand(this, TreeUtil.getPath(rootNode, it)) }

    selectInitiallySelectedDeveloperToolNode(preferredSelectedDeveloperToolNode)
  }

  private fun selectInitiallySelectedDeveloperToolNode(preferredSelectedDeveloperToolNode: ContentNode?) {
    var lastSelectedContentNodeSelected = false
    lastSelectedContentNodeId.get()?.let { lastSelectedComponentNodeId ->
      TreeUtil.promiseVisit(this) {
        val lastPathComponent = it.lastPathComponent
        if (lastPathComponent is ContentNode
          && lastPathComponent.id == lastSelectedComponentNodeId
        ) {
          INTERRUPT
        }
        else {
          CONTINUE
        }
      }.onSuccess {
        if (it != null) {
          TreeUtil.selectNode(this, it.lastPathComponent as ContentNode)
          lastSelectedContentNodeSelected = true
        }
      }
    }
    if (lastSelectedContentNodeSelected) {
      return
    }

    if (preferredSelectedDeveloperToolNode != null) {
      TreeUtil.selectNode(this, preferredSelectedDeveloperToolNode)
    }
    else {
      // Select first `DeveloperToolNode`
      TreeUtil.promiseVisit(this) { if (it.lastPathComponent is DeveloperToolNode) INTERRUPT else CONTINUE }
        .onSuccess { it?.let { TreeUtil.selectNode(this, it.lastPathComponent as DeveloperToolNode) } }
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun configureUiHelper(helper: TreeUIHelper) {
    helper.installTreeSpeedSearch(this, { path: TreePath? -> path?.lastPathComponent?.toString() }, true)
  }

  override fun setUI(ui: TreeUI?) {
    super.setUI(ui)
    val standardRowHeight = JBUI.CurrentTheme.Tree.rowHeight()
    setRowHeight(standardRowHeight + (standardRowHeight * 0.2f).toInt())
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun handleMenuTreeSelection() = TreeSelectionListener { e ->
    e.path?.lastPathComponent
      ?.safeCastTo<ContentNode>()
      ?.let {
        onContentNodeSelection(it)
        lastSelectedContentNodeId.set(it.id)
      }
  }

  private fun createTreeNodes(project: Project?, parentDisposable: Disposable): Triple<RootNode, List<GroupNode>, ContentNode?> {
    val rootNode = RootNode()

    val groupNodesToExpand = mutableListOf<GroupNode>()
    val groupNodes = mutableMapOf<String, GroupNode>()
    DeveloperToolGroup.EP_NAME.extensions.forEach { developerToolGroup ->
      val groupNode = GroupNode(developerToolGroup)
      groupNodes[developerToolGroup.id] = groupNode
      if (developerToolGroup.initiallyExpanded == true) {
        groupNodesToExpand.add(groupNode)
      }
      rootNode.add(groupNode)
    }

    var preferredSelectedDeveloperToolNode: DeveloperToolNode? = null
    val application = ApplicationManager.getApplication()
    DeveloperToolFactoryEp.EP_NAME.forEachExtensionSafe { developerToolFactoryEp ->
      val developerToolFactory: DeveloperToolFactory<*> = developerToolFactoryEp.createInstance(application)
      val developerToolConfiguration = DeveloperToolsPluginService.instance.getOrCreateDeveloperToolConfiguration(developerToolFactoryEp.id)

      developerToolFactory.getDeveloperToolCreator(developerToolConfiguration, project, parentDisposable)?.let { developerToolCreator ->
        val groupId: String? = developerToolFactoryEp.groupId
        val parentNode = if (groupId != null) (groupNodes[groupId] ?: error("Unknown group: $groupId")) else rootNode
        val developerToolNode = DeveloperToolNode(
          developerToolId = developerToolFactoryEp.id,
          parentDisposable = parentDisposable,
          developerToolContext = developerToolFactory.getDeveloperToolContext(),
          developerToolCreator = developerToolCreator,
          weight = checkNotNull(developerToolFactoryEp.weight) { "No weight set" }
        )
        parentNode.add(developerToolNode)

        if (developerToolFactoryEp.preferredSelected) {
          check(preferredSelectedDeveloperToolNode == null) { "Multiple initial selected developer tools" }
          preferredSelectedDeveloperToolNode = developerToolNode
        }
      }
    }

    rootNode.sortChildren()

    return Triple(rootNode, groupNodesToExpand, preferredSelectedDeveloperToolNode)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MenuTreeNodeRenderer(private val root: DefaultMutableTreeNode) : NodeRenderer() {

    override fun customizeCellRenderer(tree: JTree,
                                       value: Any?,
                                       selected: Boolean,
                                       expanded: Boolean,
                                       leaf: Boolean,
                                       row: Int,
                                       hasFocus: Boolean) {
      val isTopLevelNode = value is DefaultMutableTreeNode && value.parent == root
      val textAttributes = if (isTopLevelNode) REGULAR_BOLD_ATTRIBUTES else REGULAR_ATTRIBUTES
      append(value.toString(), textAttributes)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val LAST_SELECTED_CONTENT_NODE_ID_PROPERTY_KEY = "mainMenuTree_lastSelectedContentNodeId"
  }
}