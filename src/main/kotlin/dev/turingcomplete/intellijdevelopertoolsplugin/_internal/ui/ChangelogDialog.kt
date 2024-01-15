package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.UiUtils
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.ToolsMenuTree
import java.awt.Font
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
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    title = "Developer Tools - What's New"
    setSize(600, 650)
    isModal = true
    init()
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun createCenterPanel(): JComponent = BorderLayoutPanel(0, UIUtil.LARGE_VGAP).apply {
    addToCenter(object : HtmlPanel() {

      init {
        text = ToolsMenuTree::class.java.getResource(CHANGELOG_HTML_FILE)?.readText() ?: "Couldn't find 'What's New' text"
        background = UIUtil.getPanelBackground()
      }

      override fun getBodyFont(): Font = StartupUiUtil.labelFont

      override fun getBody(): String = ""

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

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val CHANGELOG_HTML_FILE = "/dev/turingcomplete/intellijdevelopertoolsplugin/changelog.html"
  }
}