package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredSideBorder
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.render.RenderingUtil
import com.intellij.ui.tree.TreeVisitor.Action.CONTINUE
import com.intellij.ui.tree.TreeVisitor.Action.INTERRUPT
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.common.safeCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.generalSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactoryEp
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.AboutPluginDialog
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.OpenSettingsAction
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.ScrollPaneConstants
import javax.swing.event.HyperlinkEvent
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.event.TreeSelectionListener
import javax.swing.plaf.TreeUI
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class ToolsMenuTree(
  private val project: Project?,
  private val parentDisposable: Disposable,
  private val settings: DeveloperToolsInstanceSettings,
  private val groupNodeSelectionEnabled: Boolean = true,
  private val prioritizeVerticalLayout: Boolean = false,
  private val selectContentNode: (ContentNode, Boolean) -> Unit,
) : SimpleTree() {
  // -- Properties ---------------------------------------------------------- //

  private var selectionTriggeredBySearch = false

  // -- Initialization ------------------------------------------------------ //

  init {
    val (rootNode, defaultGroupNodesToExpand, preferredSelectedDeveloperToolNode) =
      createTreeNodes()

    model = DefaultTreeModel(rootNode)
    setCellRenderer(MenuTreeNodeRenderer(generalSettings.toolsMenuTreeShowGroupNodes.get()))
    setTreeBorder()
    putClientProperty(RenderingUtil.ALWAYS_PAINT_SELECTION_AS_FOCUSED, true)
    background = UIUtil.SIDE_PANEL_BACKGROUND
    inputMap.clear()
    TreeUtil.installActions(this)
    isOpaque = true
    selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    addTreeSelectionListener(createTreeSelectionListener())
    addTreeExpansionListener(createTreeExpansionListener())
    isRootVisible = false
    setExpandableItemsEnabled(false)

    expandGroupNodes(defaultGroupNodesToExpand)

    selectInitiallySelectedDeveloperToolNode(preferredSelectedDeveloperToolNode)
  }

  // -- Exposed Methods ----------------------------------------------------- //

  fun recreateTreeNodes() {
    val (newRootNode, defaultGroupNodesToExpand, _) = createTreeNodes()
    (model as DefaultTreeModel).setRoot(newRootNode)
    expandGroupNodes(defaultGroupNodesToExpand)

    setCellRenderer(MenuTreeNodeRenderer(generalSettings.toolsMenuTreeShowGroupNodes.get()))
    setTreeBorder()

    revalidate()
    repaint()
  }

  override fun configureUiHelper(helper: TreeUIHelper) {
    MenuTreeSearch(this) { selectionTriggeredBySearch = it }.apply { setupListeners() }
  }

  override fun setUI(ui: TreeUI?) {
    super.setUI(ui)
    val standardRowHeight = JBUI.CurrentTheme.Tree.rowHeight()
    setRowHeight(standardRowHeight + (standardRowHeight * 0.2f).toInt())
  }

  fun createWrapperComponent(parentComponent: JComponent): JComponent {
    val wrapper =
      BorderLayoutPanel().apply {
        border = Borders.empty()
        addToCenter(this@ToolsMenuTree)
        addToBottom(
          BorderLayoutPanel().apply {
            border = Borders.empty(UIUtil.PANEL_REGULAR_INSETS)
            background = UIUtil.SIDE_PANEL_BACKGROUND
            val linksPanel =
              JPanel(VerticalLayout(UIUtil.DEFAULT_VGAP * 2)).apply {
                background = UIUtil.SIDE_PANEL_BACKGROUND
                add(createSettingsLink())
                add(createWhatsNewLink(parentComponent))
              }
            addToCenter(linksPanel)
          }
        )
      }
    return ScrollPaneFactory.createScrollPane(wrapper, true).apply {
      border =
        ColoredSideBorder(
          null,
          null,
          null,
          JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(),
          1,
        )
      background = UIUtil.SIDE_PANEL_BACKGROUND
      viewport.background = UIUtil.SIDE_PANEL_BACKGROUND
      verticalScrollBar.background = UIUtil.SIDE_PANEL_BACKGROUND
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    }
  }

  fun selectDeveloperTool(developerToolId: String, onSuccess: () -> Unit) {
    TreeUtil.promiseVisit(this) {
        val lastPathComponent = it.lastPathComponent
        if (lastPathComponent is ContentNode && lastPathComponent.id == developerToolId) {
          INTERRUPT
        } else {
          CONTINUE
        }
      }
      .onSuccess {
        if (it != null) {
          TreeUtil.selectPath(this, TreeUtil.getPathFromRoot(it.lastPathComponent as ContentNode))
            .doWhenDone { onSuccess() }
        } else {
          throw IllegalStateException("Can't find developer tool with ID: $developerToolId")
        }
      }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun setTreeBorder() {
    border =
      if (generalSettings.toolsMenuTreeShowGroupNodes.get()) {
        Borders.emptyLeft(4)
      } else {
        Borders.empty()
      }
  }

  /**
   * Directly opening the [com.intellij.openapi.options.ShowSettingsUtil] will lead to the error
   * `Slow operations are prohibited on EDT` if called in the dialog. Using
   * [com.intellij.ui.components.ActionLink] will lead to an error related to the
   * `project.messageBus`.
   */
  @Suppress("DialogTitleCapitalization")
  private fun createSettingsLink() =
    HyperlinkLabel(CommonBundle.settingsTitle()).apply {
      icon = AllIcons.General.GearPlain
      addHyperlinkListener(
        object : HyperlinkAdapter() {

          override fun hyperlinkActivated(e: HyperlinkEvent) {
            OpenSettingsAction.openSettings(project)
          }
        }
      )
    }

  private fun createWhatsNewLink(parentComponent: JComponent) =
    HyperlinkLabel("About").apply {
      icon = AllIcons.Actions.IntentionBulbGrey
      addHyperlinkListener(
        object : HyperlinkAdapter() {

          override fun hyperlinkActivated(e: HyperlinkEvent) {
            ApplicationManager.getApplication().invokeLater {
              AboutPluginDialog(project, parentComponent).show()
            }
          }
        }
      )
    }

  private fun expandGroupNodes(defaultGroupNodesToExpand: List<GroupNode>) {
    val expandedGroupNodeIds =
      settings.expandedGroupNodeIds ?: defaultGroupNodesToExpand.map { it.id }.toSet()

    TreeUtil.promiseVisit(this) {
      val lastPathComponent = it.lastPathComponent
      if (lastPathComponent is GroupNode && expandedGroupNodeIds.contains(lastPathComponent.id)) {
        TreeUtil.promiseExpand(this, it)
      }
      CONTINUE
    }
  }

  private fun selectInitiallySelectedDeveloperToolNode(
    preferredSelectedDeveloperToolNode: ContentNode?
  ) {
    var lastSelectedContentNodeSelected = false
    settings.lastSelectedContentNodeId.get()?.let { lastSelectedComponentNodeId ->
      TreeUtil.promiseVisit(this) {
          val lastPathComponent = it.lastPathComponent
          if (
            lastPathComponent is ContentNode && lastPathComponent.id == lastSelectedComponentNodeId
          ) {
            INTERRUPT
          } else {
            CONTINUE
          }
        }
        .onSuccess {
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
    } else {
      // Select first `DeveloperToolNode`
      TreeUtil.promiseVisit(this) {
          if (it.lastPathComponent is DeveloperToolNode) INTERRUPT else CONTINUE
        }
        .onSuccess {
          it?.let { TreeUtil.selectNode(this, it.lastPathComponent as DeveloperToolNode) }
        }
    }
  }

  private fun createTreeSelectionListener() = TreeSelectionListener { e ->
    e.path
      ?.lastPathComponent
      ?.safeCastTo<ContentNode>()
      ?.takeIf { groupNodeSelectionEnabled || it !is GroupNode }
      ?.let {
        if (it is ExternalNode) {
          it.selected(project)
        } else {
          selectContentNode(it, selectionTriggeredBySearch)
          settings.lastSelectedContentNodeId.set(it.id)
        }
      }
  }

  private fun createTreeExpansionListener() =
    object : TreeExpansionListener {

      override fun treeExpanded(event: TreeExpansionEvent?) {
        saveState(settings)
      }

      override fun treeCollapsed(event: TreeExpansionEvent?) {
        saveState(settings)
      }

      fun saveState(settings: DeveloperToolsInstanceSettings) {
        val expandedGroupNodeIds =
          TreeUtil.collectExpandedPaths(this@ToolsMenuTree)
            .map { it.lastPathComponent }
            .filterIsInstance<GroupNode>()
            .map { it.id }
            .toSet()
        settings.setExpandedGroupNodeIds(expandedGroupNodeIds)
      }
    }

  private fun createTreeNodes(): Triple<RootNode, List<GroupNode>, ContentNode?> {
    val toolsMenuTreeShowGroupNodes = generalSettings.toolsMenuTreeShowGroupNodes.get()

    val rootNode = RootNode()

    val defaultGroupNodesToExpand = mutableListOf<GroupNode>()
    val groupNodes = mutableMapOf<String, GroupNode>()

    val favoritesGroupNode = GroupNode(DeveloperUiToolGroup().apply {
      id = "favorites"
      menuTitle = "⭐ Favorites"
      detailTitle = "Favorites"
      initiallyExpanded = true
    })


    if (toolsMenuTreeShowGroupNodes) {
      DeveloperUiToolGroup.EP_NAME.extensions.forEach { developerToolGroup ->
        val groupNode = GroupNode(developerToolGroup)
        groupNodes[developerToolGroup.id] = groupNode
        if (developerToolGroup.initiallyExpanded == true) {
          defaultGroupNodesToExpand.add(groupNode)
        }
        rootNode.add(groupNode)
      }
    }

    var preferredSelectedDeveloperToolNode: DeveloperToolNode? = null
    val application = ApplicationManager.getApplication()
    val showInternalTools = generalSettings.showInternalTools.get()
    DeveloperUiToolFactoryEp.EP_NAME.forEachExtensionSafe { developerToolFactoryEp ->
      if (developerToolFactoryEp.internalTool && !showInternalTools) {
        return@forEachExtensionSafe
      }

      val developerUiToolFactory: DeveloperUiToolFactory<*> =
        developerToolFactoryEp.createInstance(application)
      val context = DeveloperUiToolContext(developerToolFactoryEp.id, prioritizeVerticalLayout)
      developerUiToolFactory.getDeveloperUiToolCreator(project, parentDisposable, context)?.let {
        developerToolCreator ->
        val groupId: String? =
          if (toolsMenuTreeShowGroupNodes) developerToolFactoryEp.groupId else null
        val parentNode =
          if (groupId != null) (groupNodes[groupId] ?: error("Unknown group: $groupId"))
          else rootNode
        val developerToolNode =
          DeveloperToolNode(
            developerToolId = developerToolFactoryEp.id,
            project = project,
            settings = settings,
            parentDisposable = parentDisposable,
            developerUiToolPresentation = developerUiToolFactory.getDeveloperUiToolPresentation(),
            showGrouped = toolsMenuTreeShowGroupNodes,
            developerUiToolCreator = developerToolCreator,
          )
        parentNode.add(developerToolNode)

        if (developerToolNode.isFavorite) {
          val cloneNode =
            DeveloperToolNode(
              developerToolId = developerToolFactoryEp.id,
              project = project,
              settings = settings,
              parentDisposable = parentDisposable,
              developerUiToolPresentation = developerUiToolFactory.getDeveloperUiToolPresentation(),
              showGrouped = true,
              developerUiToolCreator = developerToolCreator,
            )
          favoritesGroupNode.add(cloneNode)
        }


        if (developerToolFactoryEp.preferredSelected) {
          check(preferredSelectedDeveloperToolNode == null) {
            "Multiple initial selected developer tools"
          }
          preferredSelectedDeveloperToolNode = developerToolNode
        }
      }
    }

    if( favoritesGroupNode.childCount > 0 ) {
      rootNode.add(favoritesGroupNode)
      defaultGroupNodesToExpand.add(favoritesGroupNode)
    }


    if (generalSettings.toolsMenuTreeOrderAlphabetically.get()) {
      fun sortNodeRecursively(node: ContentNode) {
        TreeUtil.sortChildren<ContentNode>(node) { o1, o2 ->
          when {
            o1.id == "favorites" -> -1
            o2.id == "favorites" -> 1
            else -> o1.title.compareTo(o2.title)
          }
        }

        for (i in 0 until node.childCount) {
          val child = node.getChildAt(i)
          if (child is ContentNode) {
            sortNodeRecursively(child)
          }
        }
      }

      sortNodeRecursively(rootNode)
    }

    return Triple(rootNode, defaultGroupNodesToExpand, preferredSelectedDeveloperToolNode)
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class MenuTreeSearch(
    tree: Tree,
    private val setSelectionTriggeredBySearch: (Boolean) -> Unit,
  ) : TreeSpeedSearch(tree, null as Void?) {

    override fun getElementText(element: Any?): String =
      (element as TreePath).lastPathComponent.toString()

    override fun selectElement(element: Any?, selectedText: String?) {
      try {
        setSelectionTriggeredBySearch(true)
        super.selectElement(element, selectedText)
      } finally {
        setSelectionTriggeredBySearch(false)
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class MenuTreeNodeRenderer(private val toolsMenuTreeShowGroupNodes: Boolean) :
    NodeRenderer() {

    override fun customizeCellRenderer(
      tree: JTree,
      value: Any,
      selected: Boolean,
      expanded: Boolean,
      leaf: Boolean,
      row: Int,
      hasFocus: Boolean,
    ) {
      val contentNode = value.uncheckedCastTo(ContentNode::class)

      val isTopLevelNode = contentNode.parent is RootNode
      val textAttributes =
        if (contentNode.id == "favorites") {
          REGULAR_BOLD_ATTRIBUTES
        } else if (toolsMenuTreeShowGroupNodes && isTopLevelNode && !contentNode.isSecondaryNode) {
          REGULAR_BOLD_ATTRIBUTES
        } else {
          REGULAR_ATTRIBUTES
        }
      append(contentNode.title, textAttributes)
      icon = contentNode.icon
      isIconOnTheRight = false
      toolTipText = contentNode.toolTipText
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
