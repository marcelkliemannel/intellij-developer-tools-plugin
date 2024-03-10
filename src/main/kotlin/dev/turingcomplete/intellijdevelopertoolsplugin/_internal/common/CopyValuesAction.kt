package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.PlatformIcons
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.PluginCommonDataKeys.SELECTED_VALUES
import java.awt.datatransfer.StringSelection
import javax.swing.Icon

class CopyValuesAction(
  private val singleValue: String = "Copy Value",
  private val pluralValue: (Int) -> String = { "Copy $it Values" },
  private val valueToString: (Any) -> String? = { it.toString() },
  icon: Icon? = PlatformIcons.COPY_ICON
) : DumbAwareAction(singleValue, null, icon) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    val selectedValues = SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")

    val nonBlankValues = selectedValues.count { valueToString(it)?.isNotBlank() ?: false }
    e.presentation.isVisible = nonBlankValues >= 1
    e.presentation.text = if (nonBlankValues > 1) pluralValue(nonBlankValues) else singleValue
  }

  override fun actionPerformed(e: AnActionEvent) {
    val values: List<Any> = SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")
    if (values.isEmpty()) {
      return
    }

    CopyPasteManager.getInstance().setContents(StringSelection(values.mapNotNull { valueToString(it) }.joinToString(System.lineSeparator())))
  }

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}