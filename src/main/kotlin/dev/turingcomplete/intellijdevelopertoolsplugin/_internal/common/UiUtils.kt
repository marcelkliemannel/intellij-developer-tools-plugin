package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.popup.PopupState
import com.intellij.util.ui.ColumnInfo
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon

object UiUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun showDiffDialog(
    title: String,
    firstTitle: String,
    secondTitle: String,
    firstText: String,
    secondText: String
  ) {
    val diffContentFactory = DiffContentFactory.getInstance()
    val firstContent = diffContentFactory.create(firstText)
    val secondContent = diffContentFactory.create(secondText)
    val request: DiffRequest = SimpleDiffRequest(title, firstContent, secondContent, firstTitle, secondTitle)
    DiffManager.getInstance().showDiff(null, request)
  }

  fun dumbAwareAction(title: String, icon: Icon? = null, action: (AnActionEvent) -> Unit) =
    object : DumbAwareAction(title, null, icon) {

      override fun actionPerformed(e: AnActionEvent) {
        action(e)
      }
    }

  @Suppress("UnstableApiUsage")
  fun actionsPopup(
    title: String,
    icon: Icon? = null,
    actions: List<AnAction>
  ): ActionGroup = object : ActionGroup(title, null, icon), DumbAware {

    private val popupState = PopupState.forPopup()

    init {
      isPopup = true
      templatePresentation.isPerformGroup = actions.isNotEmpty()
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> = actions.toTypedArray()

    override fun actionPerformed(e: AnActionEvent) {
      if (popupState.isRecentlyHidden) {
        return
      }

      val popup = JBPopupFactory
        .getInstance()
        .createActionGroupPopup(
          null,
          this,
          e.dataContext,
          JBPopupFactory.ActionSelectionAid.MNEMONICS,
          true
        )
      popupState.prepareToShow(popup)
      PopupUtil.showForActionButtonEvent(popup, e)
    }
  }

  fun createLink(title: String, url: String): HyperlinkLabel {
    return HyperlinkLabel(title).apply {
      setHyperlinkTarget(url)
    }
  }

  fun createToggleAction(
    title: String,
    isSelected: () -> Boolean,
    setSelected: (Boolean) -> Unit
  ) = object : ToggleAction(title) {
    override fun isSelected(e: AnActionEvent): Boolean = isSelected()

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      setSelected(state)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
  }

  fun createContextMenuMouseListener(
    place: String,
    actionGroup: (MouseEvent) -> ActionGroup?
  ) = object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      handleMouseEvent(e)
    }

    override fun mouseReleased(e: MouseEvent) {
      handleMouseEvent(e)
    }

    private fun handleMouseEvent(e: InputEvent) {
      if (e is MouseEvent && e.isPopupTrigger) {
        actionGroup(e)?.let {
          ActionManager.getInstance()
            .createActionPopupMenu(place, it).component
            .show(e.getComponent(), e.x, e.y)
        }
      }
    }
  }

  fun <T> simpleColumnInfo(name: String, displayValue: (T) -> String, sortValue: (T) -> Comparable<*>) =
    object : ColumnInfo<T, String>(name) {

      override fun valueOf(item: T): String = displayValue(item)

      override fun getComparator(): Comparator<T> = compareBy { sortValue(it) }
    }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}