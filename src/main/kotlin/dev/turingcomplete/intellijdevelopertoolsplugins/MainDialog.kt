package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.*
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.navigation.Place.Navigator
import com.intellij.ui.render.RenderingUtil
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.CurrentTheme.CustomFrameDecorations
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.UIUtil.PANEL_REGULAR_INSETS
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DynamicDeveloperToolsFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.GeneralDeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.encoderdecoder.EncoderDecoder
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.textescape.TextEscape
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.uuid.UuidGenerator
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.ScrollPaneConstants
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
import javax.swing.event.TreeSelectionListener
import javax.swing.plaf.TreeUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class MainDialog(private val project: Project?) : DialogWrapper(project), Navigator {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val currentDeveloperToolHolderPanel = BorderLayoutPanel()
  private val developerToolsComponents = mutableMapOf<DeveloperTool, JComponent>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    title = "Developer Tools"
    setOKButtonText("Close")
    setSize(900, 700)
    isModal = false
    isAutoAdjustable = false
    init()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createCenterPanel() = JBSplitter(0.25f).apply {
    firstComponent = ScrollPaneFactory.createScrollPane(createMenu(), true).apply {
      border = ColoredSideBorder(null, null, null, CustomFrameDecorations.separatorForeground(), 1)
    }
    secondComponent = currentDeveloperToolHolderPanel.apply {
      border = JBEmptyBorder(PANEL_REGULAR_INSETS)
    }
  }

  override fun getStyle(): DialogStyle =  DialogStyle.COMPACT

  override fun createActions() = arrayOf(okAction)

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createMenu(): JComponent {
    val menuTree = MenuTree().apply {
      val menuRoot = model.root as DefaultMutableTreeNode
      addDeveloperToolsNodes(menuRoot)
      cellRenderer = MenuTreeNodeRenderer(menuRoot)
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
    }

    return ScrollPaneFactory.createScrollPane(null, true).apply {
      setViewportView(menuTree)
      background = UIUtil.SIDE_PANEL_BACKGROUND
      viewport.background = UIUtil.SIDE_PANEL_BACKGROUND
      verticalScrollBar.background = UIUtil.SIDE_PANEL_BACKGROUND
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    }
  }

  private fun Tree.addDeveloperToolsNodes(root: DefaultMutableTreeNode) {
    val encodersDecodersNodes = collectDeveloperToolNodes("Encoders/Decoders", EncoderDecoder.EP)
    with(root) {
      add(encodersDecodersNodes)
      add(collectDeveloperToolNodes("Text Escape", TextEscape.EP))
      add(collectDeveloperToolNodes("UUID", UuidGenerator.EP))

      GeneralDeveloperTool.EP.forEachExtensionSafe {
        add(DefaultMutableTreeNode(it))
      }

      addDynamicDeveloperTools()
    }

    expandPath(TreePath(encodersDecodersNodes.path))
  }

  private fun DefaultMutableTreeNode.addDynamicDeveloperTools() {
    DynamicDeveloperToolsFactory.EP.forEachExtensionSafe { factory ->
      if (!factory.requiresProject || project != null) {
        add(DefaultMutableTreeNode(factory.title).apply {
          factory.createDeveloperTools().forEach { add(DefaultMutableTreeNode(it)) }
        })
      }
    }
  }

  private fun collectDeveloperToolNodes(title: String, extensionPoint: ExtensionPointName<out DeveloperTool>): DefaultMutableTreeNode {
    return DefaultMutableTreeNode(title).apply {
      extensionPoint.forEachExtensionSafe { pocketKnifeTool ->
        add(DefaultMutableTreeNode(pocketKnifeTool))
      }
    }
  }

  private fun handleMenuTreeSelection() = TreeSelectionListener { e ->
    e.path?.lastPathComponent
            ?.safeCastTo<DefaultMutableTreeNode>()
            ?.userObject
            ?.safeCastTo<DeveloperTool>()
            ?.let { showDeveloperTool(it) }
  }

  private fun showDeveloperTool(developerTool: DeveloperTool) {
    val developerToolComponent = developerToolsComponents.getOrPut(developerTool) {
      createDeveloperToolComponent(developerTool)
    }

    currentDeveloperToolHolderPanel.apply {
      removeAll()
      addToCenter(developerToolComponent)
      revalidate()
      repaint()
    }
    developerTool.activated()
  }

  private fun createDeveloperToolComponent(developerTool: DeveloperTool) = panel {
    row {
      label(developerTool.title).applyToComponent { font = font.sizeToXxl() }.gap(RightGap.SMALL)
      developerTool.description?.let { contextHelp(it) }
    }

    row {
      resizableRow()
      val component = developerTool.createComponent(project, disposable)
      val componentWrapper = ScrollPaneFactory.createScrollPane(component, true).apply {
        horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_ALWAYS
        verticalScrollBarPolicy = VERTICAL_SCROLLBAR_ALWAYS
      }
      cell(componentWrapper).align(Align.FILL)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MenuTree : SimpleTree() {

    override fun configureUiHelper(helper: TreeUIHelper) {
      helper.installTreeSpeedSearch(this, { path: TreePath? -> path?.lastPathComponent?.toString() }, true)
    }

    override fun setUI(ui: TreeUI?) {
      super.setUI(ui)
      val standardRowHeight = JBUI.CurrentTheme.Tree.rowHeight()
      setRowHeight(standardRowHeight + (standardRowHeight * 0.2f).toInt())
    }
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
      append(value.toString(), if (isTopLevelNode) SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}