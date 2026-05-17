package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginInfo
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.applyDefaultTabComponentInsets
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent

class AboutPluginDialog(project: Project?, parentComponent: JComponent) :
  DialogWrapper(project, parentComponent, true, IdeModalityType.IDE) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //

  init {
    title = UiToolsBundle.message("about-plugin-dialog.title", PluginInfo.pluginName)
    isModal = true
    init()
  }

  // -- Exported Methods ---------------------------------------------------- //

  override fun createCenterPanel(): JComponent {
    val panel = panel {
      row {
        val tabs =
          mapOf(
            UiToolsBundle.message("about-plugin-dialog.about-tab") to createAboutPluginComponent(),
            UiToolsBundle.message("about-plugin-dialog.changelog-tab") to createChangelogComponent(),
          )

        cell(
          JBTabbedPane().apply {
            applyDefaultTabComponentInsets()

            tabs.forEach { (title, component) ->
              // Create scroll panes with specific preferred size
              val scrollPane = ScrollPaneFactory.createScrollPane(component, true)
              scrollPane.preferredSize = Dimension(650, 500)
              addTab(title, scrollPane)
            }
          },
        )
          .align(Align.FILL)
      }
    }

    // Set preferred size on the entire panel
    panel.preferredSize = Dimension(650, 500)
    return panel
  }

  override fun createActions(): Array<Action> = arrayOf(myOKAction)

  // -- Private Methods ----------------------------------------------------- //

  private fun createAboutPluginComponent(): JComponent =
    panel {
      row { text(UiToolsBundle.message("about-plugin-dialog.thanks", PluginInfo.pluginName)) }
        .bottomGap(BottomGap.NONE)
      row { text(UiToolsBundle.message("about-plugin-dialog.version", PluginInfo.pluginVersion)) }
        .bottomGap(BottomGap.MEDIUM)

      row { text(UiToolsBundle.message("about-plugin-dialog.issue")).gap(RightGap.SMALL) }
        .bottomGap(BottomGap.NONE)
      row { comment(UiToolsBundle.message("about-plugin-dialog.scope")) }.topGap(TopGap.NONE)

      row { text(UiToolsBundle.message("about-plugin-dialog.translations")) }
        .bottomGap(BottomGap.MEDIUM)
    }
      .apply { this.border = JBEmptyBorder(12, 0, 0, 0) }

  private fun createChangelogComponent(): JComponent = panel {
    row {
      text(
        AboutPluginDialog::class.java.getResource(CHANGELOG_HTML_FILE)?.readText()
          ?: UiToolsBundle.message("about-plugin-dialog.changelog-not-found"),
      )
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val CHANGELOG_HTML_FILE =
      "/dev/turingcomplete/intellijdevelopertoolsplugin/changelog.html"
  }
}
