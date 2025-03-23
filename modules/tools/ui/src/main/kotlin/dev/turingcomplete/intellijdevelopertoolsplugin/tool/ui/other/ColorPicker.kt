package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import com.intellij.ui.colorpicker.ColorPickerBuilder
import com.intellij.ui.colorpicker.ColorPickerModel
import com.intellij.ui.colorpicker.MaterialGraphicalColorPipetteProvider
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.ui.JBUI
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.toJBColor
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.CopyAction
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.NotBlankInputValidator
import java.awt.Color
import java.util.*
import javax.swing.border.LineBorder
import kotlin.math.max
import kotlin.math.min

class ColorPicker(
  private val project: Project?,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperUiTool(parentDisposable), DataProvider {
  // -- Properties ---------------------------------------------------------- //

  private val selectedColor: ValueProperty<JBColor> = configuration.register("selectedColor", JBColor.MAGENTA.toJBColor(), INPUT)

  private val cssRgb = AtomicProperty("")
  private val cssRgbWithAlpha = AtomicProperty("")
  private val cssHex = AtomicProperty("")
  private val cssHexWithAlpha = AtomicProperty("")
  private val cssHls = AtomicProperty("")
  private val cssHlsWithAlpha = AtomicProperty("")

  private lateinit var colorPickerModel: ColorPickerModel

  // -- Initialization ------------------------------------------------------ //

  init {
    setCssValues(selectedColor.get())

    selectedColor.afterChangeConsumeEvent(parentDisposable) {
      setCssValues(it.newValue)
      if (it.id?.equals(COLOR_SELECTION_CHANGE_ID) != true) {
        colorPickerModel.setColor(it.newValue)
      }
    }
  }

  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      val colorPicker = ColorPickerBuilder(showAlpha = true, showAlphaAsPercent = true)
        .setOriginalColor(selectedColor.get())
        .withFocus()
        .addSaturationBrightnessComponent()
        .addColorAdjustPanel(MaterialGraphicalColorPipetteProvider())
        .addColorValuePanel()
        .addColorListener({ color, _ -> selectedColor.set(color.toJBColor(), COLOR_SELECTION_CHANGE_ID) }, true)
        .apply { colorPickerModel = this.model }
        .build()
        .apply {
          content.apply {
            border = LineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
          }
        }
      cell(colorPicker.content).align(Align.FILL)
    }.bottomGap(BottomGap.MEDIUM)

    group("CSS Colors") {
      row {
        label("")
          .bindText(cssRgb)
          .gap(RightGap.SMALL)

        actionButton(CopyAction(cssRgbDataKey), ColorPicker::class.java.name)
      }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      row {
        label("")
          .bindText(cssRgbWithAlpha)
          .gap(RightGap.SMALL)
        actionButton(CopyAction(cssRgbWithAlphaDataKey), ColorPicker::class.java.name)
      }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      row {
        label("")
          .bindText(cssHex)
          .gap(RightGap.SMALL)
        actionButton(CopyAction(cssHexDataKey), ColorPicker::class.java.name)
      }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      row {
        label("")
          .bindText(cssHexWithAlpha)
          .gap(RightGap.SMALL)
        actionButton(CopyAction(cssHexWithAlphaDataKey), ColorPicker::class.java.name)
      }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      row {
        label("")
          .bindText(cssHls)
          .gap(RightGap.SMALL)
        actionButton(CopyAction(cssHslDataKey), ColorPicker::class.java.name)
      }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      row {
        label("")
          .bindText(cssHlsWithAlpha)
          .gap(RightGap.SMALL)
        actionButton(CopyAction(cssHslWithAlphaDataKey), ColorPicker::class.java.name)
      }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      row {
        button("Parse CSS Color Value") {
          val inputDialog = Messages.InputDialog(
            project,
            "CSS color value:",
            PARSE_CSS_VALUE_DIALOG_TITLE,
            null,
            "",
            NotBlankInputValidator()
          )
          inputDialog.show()
          inputDialog.inputString?.let { parseCssColorValue(it) }?.let { selectedColor.set(it) }
        }
      }.topGap(TopGap.NONE)
    }
  }

  private fun parseCssColorValue(inputString: String): JBColor? = try {
    val color = org.silentsoft.csscolor4j.Color.valueOf(inputString)
    JBColor(
      Color(color.red, color.green, color.blue, (color.opacity * 255.0).toInt()),
      Color(color.red, color.green, color.blue, (color.opacity * 255.0).toInt())
    )
  }
  catch (e: IllegalArgumentException) {
    Messages.showErrorDialog(project, e.message, PARSE_CSS_VALUE_DIALOG_TITLE)
    null
  }
  catch (_: Exception) {
    Messages.showErrorDialog(project, "Unable to parse input value.", PARSE_CSS_VALUE_DIALOG_TITLE)
    null
  }

  override fun getData(dataId: String): Any? = when {
    cssRgbDataKey.`is`(dataId) -> StringUtil.stripHtml(cssRgb.get(), false)
    cssRgbWithAlphaDataKey.`is`(dataId) -> StringUtil.stripHtml(cssRgbWithAlpha.get(), false)
    cssHexDataKey.`is`(dataId) -> StringUtil.stripHtml(cssHex.get(), false)
    cssHexWithAlphaDataKey.`is`(dataId) -> StringUtil.stripHtml(cssHexWithAlpha.get(), false)
    cssHslDataKey.`is`(dataId) -> StringUtil.stripHtml(cssHls.get(), false)
    cssHslWithAlphaDataKey.`is`(dataId) -> StringUtil.stripHtml(cssHlsWithAlpha.get(), false)
    else -> null
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun setCssValues(color: Color) {
    val red = color.red
    val green = color.green
    val blue = color.blue
    val alpha = color.alpha
    cssRgb.set("<html><code><b>rgb($red, $green, $blue)</b></code></html>")
    cssRgbWithAlpha.set("<html><code><b>rgba($red, $green, $blue, ${"%.2f".format(Locale.US, alpha / 255.0)})</b></code></html>")
    cssHex.set("<html><code><b>${"#%02X%02X%02X".format(Locale.US, red, green, blue)}</b></code></html>")
    cssHexWithAlpha.set("<html><code><b>${"#%02X%02X%02X%02X".format(Locale.US, red, green, blue, alpha)}</b></code></html>")
    val hsl = rgbToHsl(red, green, blue)
    cssHls.set("<html><code><b>${"hsl(%.2f, %.2f%%, %.2f%%)".format(Locale.US, hsl[0], hsl[1] * 100, hsl[2] * 100)}</b></code></html>")
    cssHlsWithAlpha.set(
      "<html><code><b>${
        "hsla(%.2f, %.2f%%, %.2f%%, %.2f)".format(
          Locale.US,
          hsl[0],
          hsl[1] * 100,
          hsl[2] * 100,
          alpha / 255.0
        )
      }</b></code></html>"
    )
  }

  private fun rgbToHsl(red: Int, green: Int, blue: Int): FloatArray {
    val r = red / 255f
    val g = green / 255f
    val b = blue / 255f

    val max = max(max(r.toDouble(), g.toDouble()), b.toDouble()).toFloat()
    val min = min(min(r.toDouble(), g.toDouble()), b.toDouble()).toFloat()

    var h: Float
    val s: Float
    val l = (max + min) / 2

    if (max == min) {
      s = 0f
      h = s
    }
    else {
      val d = max - min
      s = if (l > 0.5) d / (2 - max - min) else d / (max + min)

      h = when (max) {
        r -> {
          (g - b) / d + (if (g < b) 6 else 0)
        }

        g -> {
          (b - r) / d + 2
        }

        else -> {
          (r - g) / d + 4
        }
      }

      h /= 6f
    }

    return floatArrayOf(h, s, l)
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<ColorPicker> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Color Picker",
      contentTitle = "Color Picker"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> ColorPicker) =
      { configuration -> ColorPicker(project, configuration, parentDisposable) }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val COLOR_SELECTION_CHANGE_ID = "colorSelection"

    private const val PARSE_CSS_VALUE_DIALOG_TITLE = "Parse CSS Color Value"

    private val cssRgbDataKey = DataKey.create<String>("cssRgb")
    private val cssRgbWithAlphaDataKey = DataKey.create<String>("cssRgbWithAlpha")
    private val cssHexDataKey = DataKey.create<String>("cssHex")
    private val cssHexWithAlphaDataKey = DataKey.create<String>("cssHexWithAlpha")
    private val cssHslDataKey = DataKey.create<String>("cssHsl")
    private val cssHslWithAlphaDataKey = DataKey.create<String>("cssHslWithAlpha")
  }
}
