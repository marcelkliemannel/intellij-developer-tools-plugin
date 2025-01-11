package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Color
import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.math.BigDecimal
import java.math.MathContext
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ToolTipManager
import javax.swing.border.CompoundBorder

// -- Properties ---------------------------------------------------------------------------------------------------- //

val longMaxValue = BigDecimal(Long.MAX_VALUE)
val longMinValue = BigDecimal(Long.MIN_VALUE)

// -- Exposed Methods ----------------------------------------------------------------------------------------------- //

/**
 * The UI DSL only verifies the range of an `intTextField` on a user input.
 */
@Suppress("UnstableApiUsage")
fun Cell<JBTextField>.validateLongValue(range: LongRange? = null) = this.apply {
  validationInfo {
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

@Suppress("UnstableApiUsage")
fun Cell<JBTextField>.validateBigDecimalValue(
  minInclusive: BigDecimal? = null,
  mathContext: MathContext = MathContext.UNLIMITED,
  toBigDecimal: (String) -> BigDecimal? = { it.toBigDecimalOrNull(mathContext) }
) = this.apply {
  validationInfo {
    if (!this@validateBigDecimalValue.component.isEnabled) {
      return@validationInfo null
    }
    val value: BigDecimal? = try {
      toBigDecimal(this@validateBigDecimalValue.component.text)
    } catch (_: Exception) {
      return@validationInfo error("Please enter a valid number")
    }
    when {
      value == null -> error("Please enter a number")
      minInclusive != null && value < minInclusive -> error("Enter a number greater than or equal to 0")
      else -> null
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

/**
 * IntelliJ's `bindIntText` will silently fail if the input is empty and will
 * not execute any other validators.
 */
@Suppress("UnstableApiUsage")
fun Cell<JBTextField>.bindLongTextImproved(property: ObservableMutableProperty<Long>) = this.apply {
  applyToComponent {
    text = property.get().toString()
    property.afterChange { text = it.toString() }
  }
  this.whenTextChangedFromUi {
    it.toLongOrNull()?.let { longValue -> property.set(longValue) }
  }
}

/**
 * IntelliJ's `bindIntText` will silently fail if the input is empty and will
 * not execute any other validators.
 */
@Suppress("UnstableApiUsage")
fun Cell<JBTextField>.bindDoubleTextImproved(property: ObservableMutableProperty<Double>) = this.apply {
  applyToComponent {
    text = property.get().toString()
    property.afterChange { text = it.toString() }
  }
  this.whenTextChangedFromUi {
    it.toDoubleOrNull()?.let { doubleValue -> property.set(doubleValue) }
  }
}

@Suppress("UnstableApiUsage")
fun Cell<JBTextField>.validateNonEmpty(errorMessage: String) = this.applyToComponent {
  validationInfo {
    if (it.text.isEmpty()) {
      ValidationInfo(errorMessage)
    }
    else {
      null
    }
  }
}

fun Cell<JBTextArea>.setValidationResultBorder() = this.applyToComponent {
  border = CompoundBorder(ValidationResultBorder(this), JBEmptyBorder(3, 5, 3, 5))
}

@Suppress("UnstableApiUsage")
fun <T : JTextField> Cell<T>.validateMinMaxValueRelation(side: ValidateMinIntValueSide, getOppositeValue: () -> Int): Cell<T> =
  this.validationInfo {
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

fun Cell<JLabel>.changeFont(scale: Float = 1.0f, style: Int = Font.PLAIN) = this.applyToComponent {
  font = JBFont.create(this.font.deriveFont(style), false).biggerOn(scale)
}

fun Cell<JLabel>.monospaceFont(scale: Float = 1.0f, style: Int = Font.PLAIN) = this.applyToComponent {
  font = JBFont.create(Font(Font.MONOSPACED, style, this.font.size)).biggerOn(scale)
}

fun EditorEx.setLanguage(language: Language) {
  val syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, null)
  highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(syntaxHighlighter, EditorColorsManager.getInstance().globalScheme)
}

fun Row.hyperLink(title: String, url: String) {
  cell(HyperlinkLabel(title).apply {
    setHyperlinkTarget(url)
  })
}

fun Color.toJBColor() = this as? JBColor ?: JBColor(this, this)

@Suppress("UNCHECKED_CAST")
fun <T> TabInfo.castedObject(): T = this.`object` as T

operator fun ObservableMutableProperty<Boolean>.not(): ObservableMutableProperty<Boolean> =
  transform({ !it }) { !it }

fun BigDecimal.isWithinLongRange(): Boolean = (this <= longMaxValue) && (this >= longMinValue)

fun Cell<JComponent>.registerDynamicToolTip(toolTipText: () -> String?) {
  this.component.toolTipText = null
  ToolTipManager.sharedInstance().registerComponent(this.component)

  this.component.addMouseListener(object : MouseAdapter() {
    override fun mouseEntered(e: MouseEvent?) {
      this@registerDynamicToolTip.component.toolTipText = toolTipText()
    }
  })
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
