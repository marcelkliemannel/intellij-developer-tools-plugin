package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.ToolsMenuTree
import javax.swing.Action
import javax.swing.JComponent

class ChangelogDialog(
  project: Project?,
  parentComponent: JComponent
) : DialogWrapper(
  project,
  parentComponent,
  true,
  IdeModalityType.IDE
) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //

  init {
    title = "Developer Tools - What's New"
    setSize(600, 650)
    isModal = true
    init()
  }

  // -- Exported Methods ---------------------------------------------------- //

  override fun createCenterPanel(): JComponent = BorderLayoutPanel(0, UIUtil.LARGE_VGAP).apply {
    addToCenter(panel {
      row {
        text(ToolsMenuTree::class.java.getResource(CHANGELOG_HTML_FILE)?.readText() ?: "Couldn't find 'What's New' text")
          .resizableColumn()
          .align(Align.FILL)
      }.resizableRow()
    }.let { ScrollPaneFactory.createScrollPane(it, true) })

    addToBottom(
      BorderLayoutPanel().apply {
        addToLeft(
          UiUtils.createLink(
            title = "Make a feature request or report an issue",
            url = "https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues"
          )
        )
      }
    )
  }

  override fun createActions(): Array<Action> = arrayOf(myOKAction)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val CHANGELOG_HTML_FILE = "/dev/turingcomplete/intellijdevelopertoolsplugin/changelog.html"
  }
}
