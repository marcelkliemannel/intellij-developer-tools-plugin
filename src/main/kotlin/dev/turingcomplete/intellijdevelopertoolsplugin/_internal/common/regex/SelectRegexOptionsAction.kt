package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.regex

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other.RegularExpressionMatcher
import javax.swing.JComponent

class SelectRegexOptionsAction(
  private val parentComponent: () -> JComponent,
  private val selectedRegexOptionFlag: ObservableMutableProperty<Int>
) : DumbAwareAction("Regular Expression Options", null, AllIcons.General.GearPlain) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var currentDialog: Balloon? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    if (currentDialog != null) {
      return
    }

    currentDialog = JBPopupFactory.getInstance().createBalloonBuilder(createRegexOptionPanel())
      .setDialogMode(true)
      .setFillColor(UIUtil.getPanelBackground())
      .setBorderColor(JBColor.border())
      .setBlockClicksThroughBalloon(true)
      .setRequestFocus(true)
      .createBalloon()
      .apply {
        setAnimationEnabled(false)
        addListener(object : JBPopupListener {
          override fun onClosed(event: LightweightWindowEvent) {
            currentDialog = null
          }
        })
        show(RelativePoint.getSouthOf(parentComponent()), Balloon.Position.below)
      }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  private fun createRegexOptionPanel() = panel {
    val regexOptionCheckBox = mutableMapOf<RegexOption, JBCheckBox>()

    val setSelectedRegexOptionFlag: () -> Unit = {
      selectedRegexOptionFlag.set(regexOptionCheckBox.filter { it.value.isSelected }.map { it.key.patternFlag }.sum())
    }

    val selectedRegexOptionFlag = selectedRegexOptionFlag.get()
    RegexOption.entries.forEach { regexOption ->
      row {
        regexOptionCheckBox[regexOption] = checkBox(regexOption.title)
          .comment(regexOption.description)
          .applyToComponent { isSelected = regexOption.isSelected(selectedRegexOptionFlag) }
          .whenStateChangedFromUi { setSelectedRegexOptionFlag() }
          .component
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    fun createActionButton(selectedRegexOptionFlag: ObservableMutableProperty<Int>): ActionButton {
      lateinit var actionButton: ActionButton
      actionButton = ActionButton(
        SelectRegexOptionsAction({ actionButton }, selectedRegexOptionFlag),
        null,
        RegularExpressionMatcher::class.java.name,
        DEFAULT_MINIMUM_BUTTON_SIZE
      )
      return actionButton
    }
  }
}