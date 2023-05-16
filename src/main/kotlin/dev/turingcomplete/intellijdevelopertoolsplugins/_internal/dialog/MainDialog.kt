package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredSideBorder
import com.intellij.ui.JBSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.navigation.Place
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog.structure.ConfigurationNode
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog.structure.ContentNode
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog.structure.DeveloperToolNode
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog.structure.GroupNode
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

internal class MainDialog(private val project: Project?) : DialogWrapper(project), Place.Navigator {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedContentNode: ContentNode? by Delegates.observable(null, handleContentNodeSelection())
  private val contentPanel = BorderLayoutPanel()
  private val groupsPanels = mutableMapOf<String, GroupContentPanel>()
  private val developerToolsPanels = mutableMapOf<DeveloperToolNode, DeveloperToolContentPanel>()
  private val configurationPanel = ConfigurationContentPanel()
  private lateinit var mainMenuTree: MainMenuTree

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    title = "Developer Tools"
    setSize(950, 705)
    isModal = false
    isAutoAdjustable = false
    init()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createCenterPanel(): JBSplitter {
    mainMenuTree = MainMenuTree({ selectedContentNode = it }, project, disposable)

    return JBSplitter(0.25f).apply {
      dividerWidth = DIVIDER_WIDTH

      firstComponent = ScrollPaneFactory.createScrollPane(mainMenuTree, true).apply {
        border = ColoredSideBorder(null, null, null, JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1)
        background = UIUtil.SIDE_PANEL_BACKGROUND
        viewport.background = UIUtil.SIDE_PANEL_BACKGROUND
        verticalScrollBar.background = UIUtil.SIDE_PANEL_BACKGROUND
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      }

      secondComponent = contentPanel.apply {
        border = JBEmptyBorder(0, 0, 0, DIVIDER_WIDTH)
      }
    }
  }

  override fun getStyle(): DialogStyle = DialogStyle.COMPACT

  override fun createActions() = emptyArray<Action>()

  override fun createSouthPanel(): JComponent? = null

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun handleContentNodeSelection(): (KProperty<*>, ContentNode?, ContentNode?) -> Unit = { _, old, new ->
    if (old != new) {
      if (old is DeveloperToolNode) {
        developerToolsPanels[old]?.deselected()
      }

      if (new != null) {
        val nodePanel: JPanel = when (new) {
          is GroupNode -> {
            groupsPanels.getOrPut(new.developerToolGroup.id) {
              GroupContentPanel(new) {
                selectedContentNode = it
                TreeUtil.selectNode(mainMenuTree, it)
              }
            }.panel
          }

          is DeveloperToolNode -> {
            developerToolsPanels.getOrPut(new) { DeveloperToolContentPanel(new) }.also { it.selected() }
          }

          is ConfigurationNode -> configurationPanel.panel

          else -> error("Unexpected menu node: ${new::class}")
        }
        contentPanel.apply {
          removeAll()
          addToCenter(nodePanel)
          revalidate()
          repaint()
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    const val DIVIDER_WIDTH = 4
  }
}