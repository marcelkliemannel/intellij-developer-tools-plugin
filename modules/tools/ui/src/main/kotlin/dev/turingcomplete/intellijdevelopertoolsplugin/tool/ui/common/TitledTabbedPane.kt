package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.ide.ui.laf.darcula.ui.DarculaTabbedPaneUI
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import java.awt.Font
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.JPanel

class TitledTabbedPane(title: String, tabs: List<Pair<String, JComponent>>) : JBTabbedPane() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //

  init {
    tabComponentInsets = JBUI.insetsTop(8)

    addTab("", JPanel())
    setTabComponentAt(0, JBLabel(title).apply { font = font?.deriveFont(Font.BOLD) })
    setEnabledAt(0, false)

    tabs.forEach { addTab(it.first, it.second) }

    selectedIndex = 1
    setUI(TitleTabAwareTabbedPaneUi())
  }

  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class TitleTabAwareTabbedPaneUi : DarculaTabbedPaneUI() {

    override fun paintTabBackground(
      g: Graphics,
      tabPlacement: Int,
      tabIndex: Int,
      x: Int,
      y: Int,
      w: Int,
      h: Int,
      isSelected: Boolean,
    ) {
      if (tabIndex >= 1) {
        super.paintTabBackground(g, tabPlacement, tabIndex, x, y, w, h, isSelected)
      } else {
        // Remove hover on the title tab
        g.color = tabPane.getBackground()

        // The following logic was copied from the super method
        var updatedW = w
        var updatedH = h
        if (tabPane.tabLayoutPolicy == SCROLL_TAB_LAYOUT) {
          if (tabPlacement == LEFT || tabPlacement == RIGHT) {
            updatedW -= offset
          } else {
            updatedH -= offset
          }
        }

        g.fillRect(x, y, updatedW, updatedH)
      }
    }
  }

  // -- Companion Object ---------------------------------------------------- //
}
