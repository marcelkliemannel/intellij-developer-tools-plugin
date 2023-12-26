package dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.content

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.menu.ContentNode
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.menu.DeveloperToolNode
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.ui.menu.GroupNode
import org.jdesktop.swingx.VerticalLayout
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

@Suppress("DialogTitleCapitalization")
internal class GroupContentPanel(groupNode: GroupNode, private val onContentNodeSelection: (ContentNode) -> Unit) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val panel: DialogPanel = panel {
    row {
      label(groupNode.developerToolGroup.detailTitle).applyToComponent { font = JBFont.label().asBold() }
      bottomGap(BottomGap.NONE)
    }

    indent {
      row {
        val component = createDeveloperToolLinksPanel(groupNode)
        val componentWrapper = ScrollPaneFactory.createScrollPane(component, true).apply {
          horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
          verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        }
        cell(componentWrapper).align(Align.FILL)
      }.resizableRow()
    }
  }.apply { border = JBEmptyBorder(0, 8, 0, 8) }

  private fun createDeveloperToolLinksPanel(groupNode: GroupNode) = object : JPanel(VerticalLayout(UIUtil.DEFAULT_VGAP)) {
    init {
      groupNode.children().asSequence().filterIsInstance(DeveloperToolNode::class.java).forEach { developerToolNode ->
        add(ActionLink(developerToolNode.developerToolPresentation.menuTitle) { onContentNodeSelection(developerToolNode) })
      }
    }
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}