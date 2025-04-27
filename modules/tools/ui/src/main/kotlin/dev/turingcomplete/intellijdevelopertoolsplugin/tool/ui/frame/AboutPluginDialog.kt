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
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent

class AboutPluginDialog(project: Project?, parentComponent: JComponent) :
  DialogWrapper(project, parentComponent, true, IdeModalityType.IDE) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //

  init {
    title = "${PluginInfo.pluginName} - About Plugin"
    isModal = true
    init()
  }

  // -- Exported Methods ---------------------------------------------------- //

  override fun createCenterPanel(): JComponent {
    val panel = panel {
      row {
        val tabs =
          mapOf("About" to createAboutPluginComponent(), "Changelog" to createChangelogComponent())

        cell(
            JBTabbedPane().apply {
              tabs.forEach { (title, component) ->
                // Create scroll panes with specific preferred size
                val scrollPane = ScrollPaneFactory.createScrollPane(component, true)
                scrollPane.preferredSize = Dimension(650, 500)
                addTab(title, scrollPane)
              }
            }
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
        row { text("<b>Thanks for using the ${PluginInfo.pluginName} plugin! ❤</b>") }
          .bottomGap(BottomGap.NONE)
        row { text("Version: ${PluginInfo.pluginVersion}") }.bottomGap(BottomGap.MEDIUM)

        row {
            text(
                "Spotted a bug or thought of a new feature? <a href='https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues'>Please create an issue on GitHub.</a>"
              )
              .gap(RightGap.SMALL)
          }
          .bottomGap(BottomGap.NONE)
        row {
            comment(
              "(The plugin is intended for all JetBrains IDEs and will therefore not be extended with features that target a specific programming language or framework.)"
            )
          }
          .topGap(TopGap.NONE)
      }
      .apply { this.border = JBEmptyBorder(12, 0, 0, 0) }

  private fun createChangelogComponent(): JComponent = panel {
    row {
      text(
        AboutPluginDialog::class.java.getResource(CHANGELOG_HTML_FILE)?.readText()
          ?: "Couldn't find 'What's New' text"
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
