package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredSideBorder
import com.intellij.ui.JBSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.STYLE_BOLD
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI.CurrentTheme.CustomFrameDecorations
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.UIUtil.PANEL_REGULAR_INSETS
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DynamicDeveloperToolsFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.GeneralDeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.encoderdecoder.EncoderDecoder
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.textescape.TextEscape
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.uuid.UuidGenerator
import java.awt.Color
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class MainDialog(private val project: Project?) : DialogWrapper(project) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val currentDeveloperToolHolderPanel = BorderLayoutPanel()
  private val developerToolsComponents = mutableMapOf<DeveloperTool, JComponent>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    title = "Developer Tools"
    setOKButtonText("Close")
    setSize(900, 700)
    init()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createCenterPanel() = JBSplitter(0.25f).apply {
    firstComponent = ScrollPaneFactory.createScrollPane(createMenu(), true)
    secondComponent = currentDeveloperToolHolderPanel.apply {
      border = JBEmptyBorder(UIUtil.PANEL_REGULAR_INSETS)
    }
  }

  override fun getStyle(): DialogStyle =  DialogStyle.COMPACT

  override fun createActions() = arrayOf(okAction)

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createMenu(): JComponent {
    val menuTree = buildMenuTree().apply {
      isRootVisible = false
      selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

      addTreeSelectionListener(handleMenuTreeSelection())
    }

    return ScrollPaneFactory.createScrollPane(null, true).apply {
      setViewportView(menuTree)
      background = UIUtil.SIDE_PANEL_BACKGROUND
      viewport.background = UIUtil.SIDE_PANEL_BACKGROUND
      verticalScrollBar.background = UIUtil.SIDE_PANEL_BACKGROUND
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    }
  }

  private fun buildMenuTree(): Tree = Tree().apply {
    val encodersDecodersNodes = collectDeveloperToolNodes("Encoders/Decoders", EncoderDecoder.EP)
    val root = DefaultMutableTreeNode().apply {
      add(encodersDecodersNodes)
      add(collectDeveloperToolNodes("Text Escape", TextEscape.EP))
      add(collectDeveloperToolNodes("UUID", UuidGenerator.EP))

      GeneralDeveloperTool.EP.forEachExtensionSafe {
        add(DefaultMutableTreeNode(it))
      }

      addDynamicDeveloperTools()
    }
    model = DefaultTreeModel(root)

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
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}