package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.util.ObjectUtils
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.border.LineBorder

class ValidationResultBorder(
  private val ownerComponent: JComponent,
  private val focusComponent: JComponent = ownerComponent,
) : LineBorder(defaultBorderColor, 1) {
  // -- Properties ---------------------------------------------------------- //

  private val errorBorder by lazy { JBUI.CurrentTheme.Focus.errorColor(false) }
  private val errorFocusBorder by lazy { JBUI.CurrentTheme.Focus.errorColor(true) }
  private val warningBorder by lazy { JBUI.CurrentTheme.Focus.warningColor(false) }
  private val warningFocusBorder by lazy { JBUI.CurrentTheme.Focus.warningColor(true) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun paintBorder(c: Component?, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
    val outline =
      ObjectUtils.tryCast(
        ownerComponent.getClientProperty("JComponent.outline"),
        String::class.java,
      )
    this.lineColor =
      when (outline) {
        "error" -> if (focusComponent.hasFocus()) errorFocusBorder else errorBorder
        "warning" -> if (focusComponent.hasFocus()) warningFocusBorder else warningBorder
        else -> defaultBorderColor
      }
    super.paintBorder(c, g, x, y, width, height)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  private companion object {

    private val defaultBorderColor = JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()
  }
}
