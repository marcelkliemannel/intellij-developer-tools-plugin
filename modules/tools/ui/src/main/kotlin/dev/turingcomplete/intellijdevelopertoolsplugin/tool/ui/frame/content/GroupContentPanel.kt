package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.content

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.ContentNode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.DeveloperToolNode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.GroupNode
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import org.jdesktop.swingx.VerticalLayout

class GroupContentPanel(
  groupNode: GroupNode,
  private val onContentNodeSelection: (ContentNode) -> Unit,
) {
  // -- Properties ---------------------------------------------------------- //

  val panel: DialogPanel =
    panel {
        row {
          label(groupNode.developerUiToolGroup.detailTitle).applyToComponent {
            font = JBFont.label().asBold()
          }
          bottomGap(BottomGap.NONE)
        }

        indent {
          row {
              val component = createDeveloperToolLinksPanel(groupNode)
              val componentWrapper =
                ScrollPaneFactory.createScrollPane(component, true).apply {
                  horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
                  verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                }
              cell(componentWrapper).align(Align.FILL)
            }
            .resizableRow()
        }
      }
      .apply { border = JBEmptyBorder(0, 8, 0, 8) }

  private fun createDeveloperToolLinksPanel(groupNode: GroupNode) =
    object : JPanel(VerticalLayout(UIUtil.DEFAULT_VGAP)) {
      init {
        groupNode.children().asSequence().filterIsInstance(DeveloperToolNode::class.java).forEach {
          developerToolNode ->
          add(
            ActionLink(developerToolNode.developerUiToolPresentation.groupedMenuTitle) {
              onContentNodeSelection(developerToolNode)
            }
          )
        }
      }
    }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
