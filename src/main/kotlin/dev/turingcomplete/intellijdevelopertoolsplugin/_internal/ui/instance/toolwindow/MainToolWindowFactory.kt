package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.toolwindow

import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsToolWindowSettings
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.content.ContentPanelHandler
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.content.DeveloperToolContentPanel
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.toolwindow.MainToolWindowService.Companion.toolWindowContentPanelHandlerKey
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.ContentNode
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.DeveloperToolNode
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel

internal class MainToolWindowFactory : ToolWindowFactory, DumbAware {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun init(toolWindow: ToolWindow) {
    assert(toolWindow.id == ID)

    toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "false")
    toolWindow.stripeTitle = "Developer Tools"
  }

  @Suppress("UnstableApiUsage")
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val loaderPanel = BorderLayoutPanel().apply {
      addToCenter(JLabel(CollaborationToolsUIUtil.animatedLoadingIcon))
    }
    toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(loaderPanel, "", false))

    ApplicationManager.getApplication().executeOnPooledThread {
      // See `DeveloperToolsToolWindowSettings#getInstance` for an explanation
      // why the settings must be instantiated on a background thread.
      val settings = DeveloperToolsToolWindowSettings.getInstance(project)

      ApplicationManager.getApplication().invokeLater {
        toolWindow.contentManager.removeAllContents(true)

        val contentPanelHandler = ToolWindowContentPanelHandler(settings, project, toolWindow.disposable)
        val mainContent = ContentFactory.getInstance().createContent(contentPanelHandler.contentPanel, "", false).apply {
          preferredFocusableComponent = contentPanelHandler.contentPanel
          putUserData(toolWindowContentPanelHandlerKey, contentPanelHandler)
        }
        toolWindow.contentManager.addContent(mainContent)
        toolWindow.contentManager.setSelectedContent(mainContent)

        project.service<MainToolWindowService>().setToolWindow(toolWindow)
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ToolWindowDeveloperToolContentPanel(
    developerToolNode: DeveloperToolNode,
    private val toggleMenu: (JComponent) -> Unit
  ) : DeveloperToolContentPanel(developerToolNode) {

    override fun Row.buildTitle(): JComponent {
      lateinit var toggleMenuActionLink: JComponent
      val toggleMenuAction: DumbAwareAction = object : DumbAwareAction("Show Developer Tools", null, AllIcons.Actions.ListFiles) {

        override fun actionPerformed(e: AnActionEvent) {
          toggleMenu(toggleMenuActionLink)
        }
      }
      toggleMenuActionLink = actionButton(toggleMenuAction)
        .gap(RightGap.SMALL)
        .component

      @Suppress("DialogTitleCapitalization")
      return label(developerToolNode.developerUiToolPresentation.contentTitle)
        .applyToComponent { formatTitle() }
        .component
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ToolWindowContentPanelHandler(
    settings: DeveloperToolsToolWindowSettings,
    project: Project,
    parentDisposable: Disposable
  ) : ContentPanelHandler(
    project = project,
    parentDisposable = parentDisposable,
    settings = settings,
    groupNodeSelectionEnabled = false,
    promoteMainDialog = true,
    prioritizeVerticalLayout = true
  ) {

    private var lastToolsMenuTreePopup: JBPopup? = null
    private var toolsMenuTreeWrapper: JComponent?
    private var lastApplicationSettingsModificationsCounter = DeveloperToolsApplicationSettings.instance.modificationCounter

    init {
      toolsMenuTreeWrapper = toolsMenuTree.createWrapperComponent(innerContentPanel)
      Disposer.register(parentDisposable) { toolsMenuTreeWrapper = null }
    }

    override fun createDeveloperToolContentPanel(developerToolNode: DeveloperToolNode): DeveloperToolContentPanel =
      ToolWindowDeveloperToolContentPanel(developerToolNode, showMenu())

    override fun handleContentNodeSelection(new: ContentNode?, selectionTriggeredBySearch: Boolean) {
      super.handleContentNodeSelection(new, selectionTriggeredBySearch)

      if (!selectionTriggeredBySearch) {
        lastToolsMenuTreePopup?.takeIf { !it.isDisposed }?.cancel()
      }
    }

    private fun showMenu(): (JComponent) -> Unit = { menuOwner ->
      if (lastApplicationSettingsModificationsCounter != DeveloperToolsApplicationSettings.instance.modificationCounter) {
        toolsMenuTree.recreateTreeNodes()
        lastApplicationSettingsModificationsCounter = DeveloperToolsApplicationSettings.instance.modificationCounter
      }

      lastToolsMenuTreePopup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(toolsMenuTreeWrapper!!, toolsMenuTree)
        .setRequestFocus(true)
        .setResizable(true)
        .setMovable(true)
        .setDimensionServiceKey(project, TOOLS_MENU_TREE_DIMENSION_SERVICE_KEY, false)
        .setCancelOnOtherWindowOpen(true)
        .setCancelOnClickOutside(true)
        .setMinSize(Dimension(220, 200))
        .createPopup()
        .apply {
          size = Dimension(220, 600)
          Disposer.register(parentDisposable, this)
          showUnderneathOf(menuOwner)
        }

    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    const val ID = "Developer Tools"

    private const val TOOLS_MENU_TREE_DIMENSION_SERVICE_KEY = "ToolWindowDeveloperToolContentPanel"
  }
}