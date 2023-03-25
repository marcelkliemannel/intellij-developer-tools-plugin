package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.DslComponentProperty
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JTable

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //

/**
 * The UI DSL only verifies the range of an `intTextField` on a user input.
 */
fun Cell<JBTextField>.validateLongValue(range: LongRange? = null) = this.apply {
  validation {
    if (this@validateLongValue.component.isEnabled) {
      val value = this@validateLongValue.component.text.toLongOrNull()
      when {
        value == null -> error(UIBundle.message("please.enter.a.number"))
        range != null && value !in range -> error(UIBundle.message("please.enter.a.number.from.0.to.1", range.first, range.last))
        else -> null
      }
    }
    else {
      null
    }
  }
}

fun <T> Cell<JBRadioButton>.bind(property: ObservableMutableProperty<T>, value: T) = this.apply {
  this.applyToComponent {
    isSelected = property.get() == value

    this.addItemListener { event ->
      if (event.stateChange == ItemEvent.SELECTED) {
        property.set(value)
      }
    }
  }
}

/**
 * IntelliJ's `bindIntText` will silently fail if the input is empty and will
 * not execute any other validators.
 */
@Suppress("UnstableApiUsage")
fun Cell<JBTextField>.bindIntTextImproved(property: ObservableMutableProperty<Int>) = this.apply {
  applyToComponent {
    text = property.get().toString()
    property.afterChange { text = it.toString() }
  }
  this.whenTextChangedFromUi {
    it.toIntOrNull()?.let { intValue -> property.set(intValue) }
  }
}

fun validateMinMaxValueRelation(side: ValidateMinIntValueSide, getOppositeValue: () -> Int):
        ValidationInfoBuilder.(JBTextField) -> ValidationInfo? = {
  if (this.component.isEnabled) {
    it.text?.toIntOrNull()?.let { thisValue ->
      when {
        side == ValidateMinIntValueSide.MIN && thisValue > getOppositeValue() -> {
          ValidationInfo("Minimum must be smaller than or equal to maximum")
        }

        side == ValidateMinIntValueSide.MAX && thisValue < getOppositeValue() ->
          ValidationInfo("Maximum must be larger than or equal to minimum")

        else -> null
      }
    }
  }
  else {
    null
  }
}

fun JBLabel.copyable() = this.apply { setCopyable(true) }

fun JComponent.wrapWithToolBar(actionEventPlace: String, actions: ActionGroup, toolBarPlace: ToolBarPlace): JComponent {
  return BorderLayoutPanel().apply {
    val actionToolbar = ActionManager.getInstance().createActionToolbar(actionEventPlace, actions, toolBarPlace.horizontal)
    actionToolbar.targetComponent = this@wrapWithToolBar

    when (toolBarPlace) {
      ToolBarPlace.LEFT -> {
        addToLeft(actionToolbar.component)
        addToCenter(this@wrapWithToolBar)
      }

      ToolBarPlace.RIGHT, ToolBarPlace.APPEND -> {
        addToCenter(this@wrapWithToolBar)
        addToRight(actionToolbar.component)
      }
    }
  }
}

fun JComponent.allowUiDslLabel(component: JComponent = this) {
  putClientProperty(DslComponentProperty.LABEL_FOR, component)
}

fun JTable.setContextMenu(place: String, actionGroup: ActionGroup) {
  val mouseAdapter = object : MouseAdapter() {

    override fun mousePressed(e: MouseEvent) {
      handleMouseEvent(e)
    }

    override fun mouseReleased(e: MouseEvent) {
      handleMouseEvent(e)
    }

    private fun handleMouseEvent(e: InputEvent) {
      if (e is MouseEvent && e.isPopupTrigger) {
        ActionManager.getInstance()
          .createActionPopupMenu(place, actionGroup).component
          .show(e.getComponent(), e.x, e.y)
      }
    }
  }
  addMouseListener(mouseAdapter)
}

fun JBFont.toMonospace(): JBFont = JBFont.create(Font(Font.MONOSPACED, this.style, this.size))

fun EditorEx.setLanguage(language: Language) {
  val syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, null)
  highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(syntaxHighlighter, EditorColorsManager.getInstance().globalScheme)
}

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

enum class ValidateMinIntValueSide { MIN, MAX }

// -- Type ---------------------------------------------------------------------------------------------------------- //

enum class ToolBarPlace(val horizontal: Boolean) {

  LEFT(false),
  RIGHT(false),
  APPEND(true)
}
