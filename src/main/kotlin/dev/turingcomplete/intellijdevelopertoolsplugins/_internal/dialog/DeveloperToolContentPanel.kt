package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import javax.swing.ScrollPaneConstants

@Suppress("DialogTitleCapitalization")
internal class DeveloperToolContentPanel(private val developerTool: DeveloperTool) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val panel: DialogPanel = panel {
    row {
      label(developerTool.presentation.contentTitle).applyToComponent { font = JBFont.label().asBold() }
    }

    row {
      val component = developerTool.createComponent()
      val componentWrapper = ScrollPaneFactory.createScrollPane(component, true).apply {
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
      }
      cell(componentWrapper).align(Align.FILL)
    }.resizableRow()
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}