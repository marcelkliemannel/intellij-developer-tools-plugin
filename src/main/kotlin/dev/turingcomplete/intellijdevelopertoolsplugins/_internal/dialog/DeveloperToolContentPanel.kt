package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog

import com.intellij.ui.ScrollPaneFactory.createScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.components.BorderLayoutPanel
import javax.swing.JPanel

internal class DeveloperToolContentPanel(
  private val developerToolNode: DeveloperToolNode
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val panel: JPanel by lazy { createPanel() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createPanel() = object : BorderLayoutPanel() {
    init {
      addToTop(createTitleBar())
      addToCenter(createScrollPane(developerToolNode.developerTool.createComponent(), true))
    }
  }

  @Suppress("DialogTitleCapitalization")
  private fun createTitleBar() = panel {
    row {
      label(developerToolNode.developerToolContext.contentTitle)
        .applyToComponent { font = JBFont.label().asBold() }
        .align(Align.FILL)
        .resizableColumn()

      if (developerToolNode.developerToolContext.supportsReset) {
        link("Reset") { developerToolNode.developerTool.reset() }
      }
    }.resizableRow()
  }.apply { border = JBEmptyBorder(0, 8, 0, 8) }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}