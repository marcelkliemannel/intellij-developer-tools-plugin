package dev.turingcomplete.intellijdevelopertoolsplugin.common

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants

class ScrollPaneBuilder(
  private val component: JComponent
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var withoutBorder: Boolean = true
  private var verticalScrollBarPolicy: Int = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
  private var horizontalScrollBarPolicy: Int = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun withoutBorder(withoutBorder: Boolean): ScrollPaneBuilder {
    this.withoutBorder = withoutBorder
    return this
  }

  /**
   * One of:
   * - [ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED]
   * - [ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS]
   * - [ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER]
   */
  fun verticalScrollBarPolicy(verticalScrollBarPolicy: Int): ScrollPaneBuilder {
    this.verticalScrollBarPolicy = verticalScrollBarPolicy
    return this
  }

  /**
   * One of:
   * - [ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED]
   * - [ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS]
   * - [ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER]
   */
  fun horizontalScrollBarPolicy(horizontalScrollBarPolicy: Int): ScrollPaneBuilder {
    this.horizontalScrollBarPolicy = horizontalScrollBarPolicy
    return this
  }

  fun build(): JBScrollPane {
    val scrollPane = JBScrollPane(component, verticalScrollBarPolicy, horizontalScrollBarPolicy)

    if (withoutBorder) {
      scrollPane.apply {
        border = JBUI.Borders.empty()
        viewportBorder = JBUI.Borders.empty()
      }
    }

    return scrollPane
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
