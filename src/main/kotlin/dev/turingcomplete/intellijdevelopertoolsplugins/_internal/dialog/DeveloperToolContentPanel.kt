package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog

import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import javax.swing.JPanel

@Suppress("DialogTitleCapitalization")
internal class DeveloperToolContentPanel(private val developerTool: DeveloperTool) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val panel: JPanel by lazy { createPanel() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createPanel() = object: BorderLayoutPanel() {
    init {
      addToTop(createTitleBar())
      addToCenter(ScrollPaneFactory.createScrollPane(developerTool.createComponent(), true))
    }
  }

  private fun createTitleBar() = panel {
    row {
      label(developerTool.developerToolContext.contentTitle)
        .applyToComponent { font = JBFont.label().asBold() }
        .align(Align.FILL)
        .resizableColumn()

      if (developerTool.developerToolContext.supportsReset) {
        link("Reset") { developerTool.reset() }
      }
    }.resizableRow()
  }.apply { border = JBEmptyBorder(0, 8, 0, 8) }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}