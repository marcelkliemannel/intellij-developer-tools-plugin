package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import javax.swing.Icon

class SimpleToggleAction(
        text: String,
        icon: Icon?,
        private val isSelected: () -> Boolean,
        private val setSelected: (Boolean) -> Unit
) : DumbAwareToggleAction(text, "", icon) {

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun isSelected(e: AnActionEvent): Boolean = isSelected.invoke()

  override fun setSelected(e: AnActionEvent, state: Boolean) = setSelected.invoke(state)

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}