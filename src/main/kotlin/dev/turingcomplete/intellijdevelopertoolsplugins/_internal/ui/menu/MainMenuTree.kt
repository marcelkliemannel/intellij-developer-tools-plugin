package dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.menu

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
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolGroup
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolFactoryEp
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService.Companion.lastSelectedContentNodeId
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.safeCastTo
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.uncheckedCastTo
import javax.swing.JTree
import javax.swing.event.TreeSelectionListener
import javax.swing.plaf.TreeUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

internal class MainMenuTree(
  private val onContentNodeSelection: (ContentNode) -> Unit,
  project: Project?,
  parentDisposable: Disposable
) : SimpleTree() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val (rootNode, defaultGroupNodesToExpand, preferredSelectedDeveloperToolNode) = createTreeNodes(project, parentDisposable)

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

    expandNodes(defaultGroupNodesToExpand)

    selectInitiallySelectedDeveloperToolNode(preferredSelectedDeveloperToolNode)
  }

  private fun expandNodes(defaultGroupNodesToExpand: List<GroupNode>) {
    val expandedGroupNodeIds = DeveloperToolsPluginService.instance.expandedGroupNodeIds ?: defaultGroupNodesToExpand.map { it.id }.toSet()

    TreeUtil.promiseVisit(this) {
      val lastPathComponent = it.lastPathComponent
      if (lastPathComponent is GroupNode && expandedGroupNodeIds.contains(lastPathComponent.id)) {
        TreeUtil.promiseExpand(this, it)
      }
      CONTINUE
    }
  }

  private fun selectInitiallySelectedDeveloperToolNode(preferredSelectedDeveloperToolNode: ContentNode?) {
    var lastSelectedContentNodeSelected = false
    lastSelectedContentNodeId?.let { lastSelectedComponentNodeId ->
      TreeUtil.promiseVisit(this) {
        val lastPathComponent = it.lastPathComponent
        if (lastPathComponent is ContentNode && lastPathComponent.id == lastSelectedComponentNodeId) {
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
        lastSelectedContentNodeId = it.id
      }
  }

  private fun createTreeNodes(
    project: Project?,
    parentDisposable: Disposable
  ): Triple<RootNode, List<GroupNode>, ContentNode?> {
    val rootNode = RootNode()

    val defaultGroupNodesToExpand = mutableListOf<GroupNode>()
    val groupNodes = mutableMapOf<String, GroupNode>()
    DeveloperToolGroup.EP_NAME.extensions.forEach { developerToolGroup ->
      val groupNode = GroupNode(developerToolGroup)
      groupNodes[developerToolGroup.id] = groupNode
      if (developerToolGroup.initiallyExpanded == true) {
        defaultGroupNodesToExpand.add(groupNode)
      }
      rootNode.add(groupNode)
    }

    var preferredSelectedDeveloperToolNode: DeveloperToolNode? = null
    val application = ApplicationManager.getApplication()
    DeveloperToolFactoryEp.EP_NAME.forEachExtensionSafe { developerToolFactoryEp ->
      val developerToolFactory: DeveloperToolFactory<*> = developerToolFactoryEp.createInstance(application)
      val context = DeveloperToolContext(developerToolFactoryEp.id)
      developerToolFactory.getDeveloperToolCreator(project, parentDisposable, context)?.let { developerToolCreator ->
        val groupId: String? = developerToolFactoryEp.groupId
        val parentNode = if (groupId != null) (groupNodes[groupId] ?: error("Unknown group: $groupId")) else rootNode
        val developerToolNode = DeveloperToolNode(
          developerToolId = developerToolFactoryEp.id,
          project = project,
          parentDisposable = parentDisposable,
          developerToolPresentation = developerToolFactory.getDeveloperToolPresentation(),
          developerToolCreator = developerToolCreator
        )
        parentNode.add(developerToolNode)

        if (developerToolFactoryEp.preferredSelected) {
          check(preferredSelectedDeveloperToolNode == null) { "Multiple initial selected developer tools" }
          preferredSelectedDeveloperToolNode = developerToolNode
        }
      }
    }

    rootNode.add(ConfigurationNode())

    return Triple(rootNode, defaultGroupNodesToExpand, preferredSelectedDeveloperToolNode)
  }

  fun saveState() {
    val expandedGroupNodeIds = TreeUtil.collectExpandedPaths(this)
      .map { it.lastPathComponent }
      .filterIsInstance<GroupNode>()
      .map { it.id }
      .toSet()
    DeveloperToolsPluginService.instance.setExpandedGroupNodeIds(expandedGroupNodeIds)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MenuTreeNodeRenderer(private val root: DefaultMutableTreeNode) : NodeRenderer() {

    override fun customizeCellRenderer(tree: JTree,
                                       value: Any,
                                       selected: Boolean,
                                       expanded: Boolean,
                                       leaf: Boolean,
                                       row: Int,
                                       hasFocus: Boolean) {
      val contentNode = value.uncheckedCastTo(ContentNode::class)

      val isTopLevelNode = contentNode.parent == root
      val textAttributes = if (isTopLevelNode && !contentNode.isSecondaryNode) {
        REGULAR_BOLD_ATTRIBUTES
      }
      else {
        REGULAR_ATTRIBUTES
      }
      append(contentNode.title, textAttributes)
      icon = contentNode.icon
      isIconOnTheRight = true
      toolTipText = contentNode.toolTipText
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}