package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Anchor
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.IdeActions
import dev.turingcomplete.intellijdevelopertoolsplugin.common.safeCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.dialog.OpenMainDialogAction

class AddOpenMainDialogActionToMainToolbarTask(
  private val openMainDialogAction: AnAction,
  private val mainToolbarRightActionGroup: DefaultActionGroup?,
  private val mainToolbarActionGroup: DefaultActionGroup?
) : Runnable {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun run() {
    if (mainToolbarRightActionGroup != null && !mainToolbarRightActionGroup.containsAction(openMainDialogAction)) {
      mainToolbarRightActionGroup.add(openMainDialogAction, Constraints(Anchor.BEFORE, "SearchEverywhere"))
    }

    if (mainToolbarActionGroup != null && !mainToolbarActionGroup.containsAction(openMainDialogAction)) {
      mainToolbarActionGroup.add(openMainDialogAction, Constraints.LAST)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    /**
     * Notes regarding the already added check:
     * - The [DefaultActionGroup.add] in [AddOpenMainDialogActionToMainToolbarTask]
     * will not add the action if the toolbar does not exist. Each toolbar
     * exists only either in the new or old UI. So, the action will only be
     * added to the toolbar of the currently active UI.
     * - If the user activity removed the action from the toolbar, the
     * [AddOpenMainDialogActionToMainToolbarTask] will still add the action,
     * but it will be hidden (this due to the working toolbar
     * action mechanism). Therefore, the [DefaultActionGroup.containsAction]
     * returns true even though the action is not visible.
     */
    fun createIfAvailable(): AddOpenMainDialogActionToMainToolbarTask? {
      val openMainDialogAction = OpenMainDialogAction.getAction() ?: return null

      val mainToolbarRightActionGroup = ActionManager.getInstance().getAction(IdeActions.GROUP_MAIN_TOOLBAR_RIGHT)?.safeCastTo<DefaultActionGroup>() // New UI
      val mainToolbarActionGroup = ActionManager.getInstance().getAction(IdeActions.GROUP_MAIN_TOOLBAR)?.safeCastTo<DefaultActionGroup>() // Old UI
      if (mainToolbarRightActionGroup == null && mainToolbarActionGroup == null) {
        return null
      }
      if (
        mainToolbarRightActionGroup?.containsAction(openMainDialogAction) == true
        || mainToolbarActionGroup?.containsAction(openMainDialogAction) == true
      ) {
        return null
      }

      return AddOpenMainDialogActionToMainToolbarTask(
        openMainDialogAction = openMainDialogAction,
        mainToolbarRightActionGroup = mainToolbarRightActionGroup,
        mainToolbarActionGroup = mainToolbarActionGroup
      )
    }
  }
}
