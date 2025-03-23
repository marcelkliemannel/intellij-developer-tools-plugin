package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.ui.OptionAction
import com.intellij.ui.components.JBOptionButton
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

class AnActionOptionButton(mainAction: AnAction, vararg additionalActions: AnAction) :
  JBOptionButton(null, null) {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //

  init {
    action = AnActionWrapper(mainAction, this)
    options = additionalActions.map { AnActionWrapper(it, this) }.toTypedArray()
  }

  // -- Exposed Methods ----------------------------------------------------- //

  private class AnActionWrapper(private val action: AnAction, private val component: JComponent) :
    AbstractAction(action.templatePresentation.text, action.templatePresentation.icon) {

    init {
      putValue(OptionAction.AN_ACTION, action)
    }

    override fun actionPerformed(e: ActionEvent?) {
      val context: DataContext = DataManager.getInstance().getDataContext(component)
      val event =
        AnActionEvent.createEvent(action, context, null, "jvmaction", ActionUiKind.NONE, null)
      ActionUtil.performActionDumbAwareWithCallbacks(action, event)
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
