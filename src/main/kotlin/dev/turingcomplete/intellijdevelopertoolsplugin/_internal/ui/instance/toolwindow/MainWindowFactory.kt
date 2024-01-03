package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.RelativeFont
import com.intellij.ui.components.ActionLink
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Row
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsToolWindowSettings
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.content.ContentPanelHandler
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.content.DeveloperToolContentPanel
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.DeveloperToolNode
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

class MainWindowFactory : ToolWindowFactory, DumbAware {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun init(toolWindow: ToolWindow) {
    toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "false")
    toolWindow.stripeTitle = "Developer Tools"
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentPanelHandler = ToolWindowContentPanelHandler(project, toolWindow.disposable)

    with(toolWindow.contentManager) {
      val content = ContentFactory.getInstance().createContent(contentPanelHandler.contentPanel, "", false).apply {
        preferredFocusableComponent = contentPanelHandler.contentPanel
      }
      addContent(content)
      setSelectedContent(content)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ToolWindowDeveloperToolContentPanel(
    developerToolNode: DeveloperToolNode,
    private val toggleMenu: (JComponent) -> Unit
  ) : DeveloperToolContentPanel(developerToolNode) {

    override fun Row.buildTitle() {

      lateinit var toggleMenuActionLink: ActionLink
      val toggleMenuAction: AbstractAction = object : AbstractAction(developerToolNode.developerUiToolPresentation.contentTitle) {

        override fun actionPerformed(event: ActionEvent) {
          toggleMenu(toggleMenuActionLink)
        }
      }
      toggleMenuActionLink = ActionLink(toggleMenuAction).apply {
        RelativeFont.BOLD.install(this)
      }
      cell(toggleMenuActionLink)
        .applyToComponent { font = JBFont.label().asBold() }
        .align(Align.FILL)
        .resizableColumn()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ToolWindowContentPanelHandler(
    project: Project,
    parentDisposable: Disposable
  ) : ContentPanelHandler(
    project = project,
    parentDisposable = parentDisposable,
    settings = DeveloperToolsToolWindowSettings.getInstance(project),
    groupNodeSelectionEnabled = false,
    promoteMainDialog = true,
    prioritizeVerticalLayout = true
  ) {

    private var lastToolsMenuTreePopup: JBPopup? = null
    private var toolsMenuTreeWrapper: JComponent?

    init {
      toolsMenuTreeWrapper = toolsMenuTree.createWrapperComponent()
      Disposer.register(parentDisposable) { toolsMenuTreeWrapper = null }

      selectedContentNode.afterChangeConsumeEvent(parentDisposable) {
        if (DeveloperToolsApplicationSettings.instance.toolWindowMenuHideOnToolSelection) {
          lastToolsMenuTreePopup?.takeIf { !it.isDisposed }?.cancel()
        }
      }
    }

    override fun createDeveloperToolContentPanel(developerToolNode: DeveloperToolNode): DeveloperToolContentPanel =
      ToolWindowDeveloperToolContentPanel(developerToolNode, showMenu())

    private fun showMenu(): (JComponent) -> Unit = { menuOwner ->
      lastToolsMenuTreePopup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(toolsMenuTreeWrapper!!, toolsMenuTreeWrapper)
        .setRequestFocus(true)
        .setResizable(true)
        .setShowBorder(true)
        .setCancelOnClickOutside(true)
        .setMinSize(Dimension(220, 200))
        .createPopup()
        .apply {
          Disposer.register(parentDisposable, this)
          showUnderneathOf(menuOwner)
        }

    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}