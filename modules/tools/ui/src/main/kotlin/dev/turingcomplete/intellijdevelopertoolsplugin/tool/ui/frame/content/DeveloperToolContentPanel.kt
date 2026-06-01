package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.content

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages.InputDialog
import com.intellij.ui.RelativeFont
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.actionsButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.tabs.JBTabs
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.TabsListener
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.generalSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.NotBlankInputValidator
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils.dumbAwareAction
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.castedObject
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolReference
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.DeveloperToolNode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu.DeveloperToolNode.DeveloperToolContainer
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.GeneralBundle
import javax.swing.Icon
import javax.swing.JComponent
import kotlin.reflect.cast

open class DeveloperToolContentPanel(protected val developerToolNode: DeveloperToolNode) :
  BorderLayoutPanel() {
  // -- Properties ---------------------------------------------------------- //

  private lateinit var tabs: JBTabs
  private lateinit var selectedDeveloperToolInstance:
    ObservableMutableProperty<DeveloperToolContainer>

  // -- Initialization ------------------------------------------------------ //

  init {
    addToTop(createTitleBar())
    addToCenter(createMainContent())
  }

  // -- Exposed Methods ----------------------------------------------------- //

  fun selected() {
    selectedDeveloperToolInstance.get().instance.activated()
  }

  fun deselected() {
    selectedDeveloperToolInstance.get().instance.deactivated()
  }

  fun <T : OpenDeveloperToolContext> openTool(
    context: T,
    reference: OpenDeveloperToolReference<out T>,
  ) {
    val developerUiToolInstance = selectedDeveloperToolInstance.get().instance
    assert(developerUiToolInstance is OpenDeveloperToolHandler<*>)
    @Suppress("UNCHECKED_CAST")
    (developerUiToolInstance as OpenDeveloperToolHandler<T>).applyOpenDeveloperToolContext(
      reference.contextClass.cast(context)
    )
  }

  @Suppress("DialogTitleCapitalization")
  protected open fun Row.buildTitle(): JComponent {
    return label(developerToolNode.developerUiToolPresentation.contentTitle)
      .applyToComponent { formatTitle() }
      .component
  }

  protected fun JComponent.formatTitle() {
    RelativeFont.BOLD.install(this)
    RelativeFont.LARGE.install(this)
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createTitleBar(): JComponent =
    panel {
        row {
            val titleComponent = buildTitle()

            val actions =
              mutableListOf(
                dumbAwareAction(GeneralBundle.message("developer-tool-content-panel.reset")) {
                  selectedDeveloperToolInstance.get().apply {
                    configuration.reset()
                    instance.reset()
                  }
                },
                createNewWorkbenchAction(),
              )
            developerToolNode.developerUiToolPresentation.description?.let { description ->
              actions.add(
                dumbAwareAction(
                  GeneralBundle.message("developer-tool-content-panel.show-tool-description")
                ) {
                  description.show(titleComponent)
                }
              )
            }
            actionsButton(actions = actions.toTypedArray(), icon = AllIcons.General.GearPlain)
              .align(AlignX.RIGHT)
              .resizableColumn()
              .gap(RightGap.SMALL)
          }
          .resizableRow()
      }
      .apply { border = JBEmptyBorder(0, 8, 0, 8) }

  private fun createMainContent(): JComponent {
    tabs = JBTabsFactory.createTabs(developerToolNode.project, developerToolNode.parentDisposable)

    developerToolNode.developerTools.forEach { addWorkbench(it) }

    restoreTabOrder()

    selectedDeveloperToolInstance = AtomicProperty(tabs.selectedInfo!!.castedObject())

    tabs.addListener(createTabsChangedListener(), developerToolNode.parentDisposable)
    syncTabsSelectionVisibility()

    return tabs.component
  }

  private fun syncTabsSelectionVisibility() {
    tabs.presentation.isHideTabs =
      generalSettings.hideWorkbenchTabsOnSingleTab.get() && tabs.tabCount == 1
  }

  private fun createTabsChangedListener() =
    object : TabsListener {
      override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
        oldSelection?.castedObject<DeveloperToolContainer>()?.instance?.deactivated()

        if (newSelection != null) {
          val newDeveloperToolInstance = newSelection.castedObject<DeveloperToolContainer>()
          selectedDeveloperToolInstance.set(newDeveloperToolInstance)
          newDeveloperToolInstance.instance.activated()
        }
      }

      override fun tabsMoved() {
        saveTabOrder()
      }
    }

  private fun addWorkbench(developerToolContainer: DeveloperToolContainer) {
    val developerToolComponent = developerToolContainer.instance.createComponent()
    val tabInfo =
      TabInfo(developerToolComponent).apply {
        setText(developerToolContainer.configuration.name)
        setObject(developerToolContainer)

        val destroyAction = createDestroyWorkbenchAction(developerToolContainer.instance, this)
        setTabLabelActions(
          DefaultActionGroup(destroyAction),
          DeveloperToolContentPanel::class.java.name,
        )

        val newWorkbenchAction = createNewWorkbenchAction()
        setTabPaneActions(DefaultActionGroup(newWorkbenchAction))
      }
    tabs.addTab(tabInfo)
    tabs.select(tabInfo, false)
    tabs.setPopupGroup(
      DefaultActionGroup(
        createRenameWorkbenchAction(),
        createMoveTabAction(direction = -1),
        createMoveTabAction(direction = 1),
      ),
      DeveloperToolContentPanel::class.java.name,
      true,
    )
  }

  private fun createRenameWorkbenchAction() =
    object :
      DumbAwareAction(
        GeneralBundle.message("developer-tool-content-panel.rename"),
        null,
        AllIcons.Actions.Edit,
      ) {

      override fun actionPerformed(e: AnActionEvent) {
        val (_, developerToolConfiguration) = selectedDeveloperToolInstance.get()

        val inputDialog =
          InputDialog(
            developerToolNode.project,
            GeneralBundle.message("developer-tool-content-panel.new-name"),
            GeneralBundle.message("developer-tool-content-panel.rename"),
            null,
            developerToolConfiguration.name,
            NotBlankInputValidator(),
          )
        inputDialog.show()
        inputDialog.inputString?.let { newName ->
          developerToolConfiguration.name = newName
          tabs.selectedInfo?.setText(newName)
        }
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

  private fun createMoveTabAction(direction: Int) =
    object : DumbAwareAction(
      if (direction < 0) "Move Left" else "Move Right",
      null,
      if (direction < 0) AllIcons.General.ArrowLeft else AllIcons.General.ArrowRight
    ) {

      override fun update(e: AnActionEvent) {
        val selectedInfo = tabs.selectedInfo
        val currentIndex = tabs.tabs.indexOf(selectedInfo)
        val targetIndex = currentIndex + direction
        e.presentation.isEnabled = selectedInfo != null && targetIndex in 0 until tabs.tabCount
      }

      override fun actionPerformed(e: AnActionEvent) {
        val selectedInfo = tabs.selectedInfo ?: return
        val currentIndex = tabs.tabs.indexOf(selectedInfo)
        val targetIndex = currentIndex + direction

        if (targetIndex in 0 until tabs.tabCount) {
          tabs.removeTab(selectedInfo)
          tabs.addTab(selectedInfo, targetIndex)
          tabs.select(selectedInfo, true)
          saveTabOrder()
        }
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }


  private fun createDestroyWorkbenchAction(developerUiTool: DeveloperUiTool, tabInfo: TabInfo) =
    DestroyWorkbenchAction(
      {
        tabs.removeTab(tabInfo)
        developerToolNode.destroyDeveloperToolInstance(developerUiTool)
        syncTabsSelectionVisibility()
      },
      { tabs.tabs.size > 1 },
    )

  private fun createNewWorkbenchAction() =
    object :
      DumbAwareAction(
        GeneralBundle.message("developer-tool-content-panel.new-workbench"),
        null,
        AllIcons.General.Add,
      ) {

      override fun actionPerformed(e: AnActionEvent) {
        addWorkbench(developerToolNode.createNewDeveloperToolInstance())
        syncTabsSelectionVisibility()
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

  private fun restoreTabOrder() {
    val savedOrder = developerToolNode.settings.getToolTabOrder(developerToolNode.id)
    if (savedOrder.isEmpty()) {
      return
    }

    val existingTabs = mutableMapOf<String, TabInfo>()
    tabs.tabs.forEach { tab ->
      val container = tab.`object` as DeveloperToolContainer
      existingTabs[container.configuration.id.toString()] = tab
    }

    val orderedTabs = ArrayList<TabInfo>()

    savedOrder.forEach { id ->
      existingTabs[id]?.let { tab ->
        orderedTabs.add(tab)
        existingTabs.remove(id)
      }
    }

    orderedTabs.addAll(existingTabs.values)

    tabs.removeAllTabs()
    orderedTabs.forEach { tab ->
      tabs.addTab(tab)
    }
  }

  private fun saveTabOrder() {
    val order = tabs.tabs.map {
      (it.`object` as DeveloperToolContainer).configuration.id.toString()
    }
    developerToolNode.settings.setToolTabOrder(developerToolNode.id, order)
  }


  // -- Inner Type ---------------------------------------------------------- //

  private class DestroyWorkbenchAction(
    private val removeTab: () -> Unit,
    private val visible: () -> Boolean,
  ) : DumbAwareAction(GeneralBundle.message("developer-tool-content-panel.close-workbench")) {

    override fun update(e: AnActionEvent) {
      e.presentation.apply {
        isVisible = visible()
        icon = CLOSE_ICON
        hoveredIcon = CLOSE_HOVERED_ICON
      }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
      removeTab()
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val CLOSE_ICON: Icon = AllIcons.Actions.Close
    private val CLOSE_HOVERED_ICON: Icon = AllIcons.Actions.CloseHovered
  }
}
