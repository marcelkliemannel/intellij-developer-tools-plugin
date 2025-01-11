package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsConfigurable

class OpenSettingsAction : DumbAwareAction(
  "Open Developer Tools ${CommonBundle.settingsTitle()}",
  null,
  AllIcons.General.GearPlain
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    ShowSettingsUtil.getInstance().showSettingsDialog(e.project, DeveloperToolsConfigurable::class.java)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    fun openSettings(project: Project?) {
      val openSettingsAction = OpenSettingsAction()
      val dataContext = DataContext { if (CommonDataKeys.PROJECT.`is`(it)) project else null }
      val event = AnActionEvent.createEvent(openSettingsAction, dataContext, null, OpenSettingsAction::class.java.name, ActionUiKind.NONE, null)
      ActionUtil.invokeAction(openSettingsAction, event, null)
    }
  }
}