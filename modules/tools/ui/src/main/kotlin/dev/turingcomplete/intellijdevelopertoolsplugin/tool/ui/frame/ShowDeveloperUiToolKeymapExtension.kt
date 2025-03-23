package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.keymap.KeymapExtension
import com.intellij.openapi.keymap.KeymapGroup
import com.intellij.openapi.keymap.KeymapGroupFactory
import com.intellij.openapi.keymap.impl.ui.ActionsTreeUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.ui.IconManager
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.ShowDeveloperToolUtils.showDeveloperToolActions

class ShowDeveloperUiToolKeymapExtension : KeymapExtension {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun createGroup(filtered: Condition<in AnAction>?, project: Project?): KeymapGroup? {
    val group = KeymapGroupFactory.getInstance().createGroup("Show Developer Tool", icon)

    for (action in showDeveloperToolActions) {
      ActionsTreeUtil.addAction(group, action, filtered)
    }

    return group
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  private val icon = IconManager.getInstance().getIcon("dev/turingcomplete/intellijdevelopertoolsplugin/icons/action.svg", ShowDeveloperUiToolKeymapExtension::class.java.classLoader)
}
