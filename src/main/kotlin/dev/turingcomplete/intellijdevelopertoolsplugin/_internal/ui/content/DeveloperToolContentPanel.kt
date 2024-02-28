package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.content

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages.InputDialog
import com.intellij.ui.RelativeFont
import com.intellij.ui.ScrollPaneFactory.createScrollPane
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
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.NotBlankInputValidator
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.UiUtils.dumbAwareAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.castedObject
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.handling.OpenDeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.handling.OpenDeveloperToolHandler
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.handling.OpenDeveloperToolReference
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.DeveloperToolNode
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.menu.DeveloperToolNode.DeveloperToolContainer
import javax.swing.Icon
import javax.swing.JComponent
import kotlin.reflect.cast

internal open class DeveloperToolContentPanel(
  protected val developerToolNode: DeveloperToolNode
) : BorderLayoutPanel() {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var tabs: JBTabs
  private lateinit var selectedDeveloperToolInstance: ObservableMutableProperty<DeveloperToolContainer>

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    addToTop(createTitleBar())
    addToCenter(createMainContent())
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun selected() {
    selectedDeveloperToolInstance.get().instance.activated()
  }

  fun deselected() {
    selectedDeveloperToolInstance.get().instance.deactivated()
  }

  fun <T: OpenDeveloperToolContext> openTool(context: T, reference: OpenDeveloperToolReference<out T>) {
    val developerUiToolInstance = selectedDeveloperToolInstance.get().instance
    assert(developerUiToolInstance is OpenDeveloperToolHandler<*>)
    @Suppress("UNCHECKED_CAST")
    (developerUiToolInstance as OpenDeveloperToolHandler<T>).applyOpenDeveloperToolContext(reference.contextClass.cast(context))
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

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createTitleBar(): JComponent = panel {
    row {
      val titleComponent = buildTitle()

      val actions = mutableListOf(
        dumbAwareAction("Reset") {
          selectedDeveloperToolInstance.get().apply {
            configuration.reset()
            instance.reset()
          }
        }
      )
      developerToolNode.developerUiToolPresentation.description?.let { description ->
        actions.add(
          dumbAwareAction("Show Tool Description") {
            description.show(titleComponent)
          }
        )
      }
      actionsButton(
        actions = actions.toTypedArray(),
        icon = AllIcons.General.GearPlain
      ).align(AlignX.RIGHT).resizableColumn().gap(RightGap.SMALL)
    }.resizableRow()
  }.apply { border = JBEmptyBorder(0, 8, 4, 8) }

  private fun createMainContent(): JComponent {
    tabs = JBTabsFactory.createTabs(developerToolNode.project, developerToolNode.parentDisposable)

    developerToolNode.developerTools.forEach { addWorkbench(it) }
    selectedDeveloperToolInstance = AtomicProperty(tabs.selectedInfo!!.castedObject())

    tabs.addListener(createTabsChangedListener(), developerToolNode.parentDisposable)

    return tabs.component
  }

  private fun createTabsChangedListener() = object : TabsListener {
    override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
      oldSelection?.castedObject<DeveloperToolContainer>()?.instance?.deactivated()

      if (newSelection != null) {
        val newDeveloperToolInstance = newSelection.castedObject<DeveloperToolContainer>()
        selectedDeveloperToolInstance.set(newDeveloperToolInstance)
        newDeveloperToolInstance.instance.activated()
      }
    }
  }

  private fun addWorkbench(developerToolContainer: DeveloperToolContainer) {
    val developerToolComponent = developerToolContainer.instance.createComponent().run {
      createScrollPane(this, true)
    }
    val tabInfo = TabInfo(developerToolComponent).apply {
      text = developerToolContainer.configuration.name
      `object` = developerToolContainer

      val destroyAction = createDestroyWorkbenchAction(developerToolContainer.instance, this)
      setTabLabelActions(DefaultActionGroup(destroyAction), DeveloperToolContentPanel::class.java.name)

      val newWorkbenchAction = createNewWorkbenchAction()
      tabPaneActions = DefaultActionGroup(newWorkbenchAction)
    }
    tabs.addTab(tabInfo)
    tabs.select(tabInfo, false)
    tabs.setPopupGroup(
      DefaultActionGroup(createRenameWorkbenchAction()), DeveloperToolContentPanel::class.java.name, true
    )
  }

  private fun createRenameWorkbenchAction() =
    object : DumbAwareAction("Rename", null, AllIcons.Actions.Edit) {

      override fun actionPerformed(e: AnActionEvent) {
        val (_, developerToolConfiguration) = selectedDeveloperToolInstance.get()

        val inputDialog = InputDialog(
          developerToolNode.project,
          "New name:",
          "Rename",
          null,
          developerToolConfiguration.name,
          NotBlankInputValidator()
        )
        inputDialog.show()
        inputDialog.inputString?.let { newName ->
          developerToolConfiguration.name = newName
          tabs.selectedInfo?.let { it.text = newName }
        }
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

  private fun createDestroyWorkbenchAction(developerUiTool: DeveloperUiTool, tabInfo: TabInfo) =
    DestroyWorkbenchAction(
      {
        tabs.removeTab(tabInfo)
        developerToolNode.destroyDeveloperToolInstance(developerUiTool)
      },
      { tabs.tabs.size > 1 }
    )

  private fun createNewWorkbenchAction() =
    object : DumbAwareAction("New Workbench", null, AllIcons.General.Add) {

      override fun actionPerformed(e: AnActionEvent) {
        addWorkbench(developerToolNode.createNewDeveloperToolInstance())
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class DestroyWorkbenchAction(
    private val removeTab: () -> Unit,
    private val visible: () -> Boolean,
  ) : DumbAwareAction("Close Workbench") {

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

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val CLOSE_ICON: Icon = AllIcons.Actions.Close
    private val CLOSE_HOVERED_ICON: Icon = AllIcons.Actions.CloseHovered
  }
}