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
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.util.ui.JBUI
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.CopyAction
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.NotBlankInputValidator
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.toJBColor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.awt.Color
import java.util.Locale
import javax.swing.border.LineBorder

class ColorPicker(
  private val project: Project?,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
) : DeveloperUiTool(parentDisposable), DataProvider {
  // -- Properties ---------------------------------------------------------- //

  private val selectedColor: ValueProperty<JBColor> =
    configuration.register("selectedColor", JBColor.MAGENTA.toJBColor(), INPUT)

  private val decimalPlaces: ValueProperty<Int> =
    configuration.register("decimalPlaces", 2, CONFIGURATION)

  private val cssRgb = AtomicProperty("")
  private val cssRgbWithAlpha = AtomicProperty("")
  private val cssHex = AtomicProperty("")
  private val cssHexWithAlpha = AtomicProperty("")
  private val cssHls = AtomicProperty("")
  private val cssHlsWithAlpha = AtomicProperty("")

  private lateinit var colorPickerModel: ColorPickerModel

  // -- Initialization ------------------------------------------------------ //

  init {
    selectedColor.afterChangeConsumeEvent(parentDisposable) {
      setCssValues()
      if (it.id?.equals(COLOR_SELECTION_CHANGE_ID) != true) {
        colorPickerModel.setColor(it.newValue)
      }
    }
  }

  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
        val colorPicker =
          ColorPickerBuilder(showAlpha = true, showAlphaAsPercent = true)
            .setOriginalColor(selectedColor.get())
            .withFocus()
            .addSaturationBrightnessComponent()
            .addColorAdjustPanel(MaterialGraphicalColorPipetteProvider())
            .addColorValuePanel()
            .addColorListener(
              { color, _ -> selectedColor.set(color.toJBColor(), COLOR_SELECTION_CHANGE_ID) },
              true,
            )
            .apply { colorPickerModel = this.model }
            .build()
            .apply {
              content.apply {
                border = LineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
              }
            }
        cell(colorPicker.content).align(Align.FILL)
      }
      .bottomGap(BottomGap.MEDIUM)

    group(UiToolsBundle.message("color-picker.css-colors.title")) {
      row {
          label("").bindText(cssRgb).gap(RightGap.SMALL)

          actionButton(CopyAction(cssRgbDataKey), ColorPicker::class.java.name)
        }
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      row {
          label("").bindText(cssRgbWithAlpha).gap(RightGap.SMALL)
          actionButton(CopyAction(cssRgbWithAlphaDataKey), ColorPicker::class.java.name)
        }
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      row {
          label("").bindText(cssHex).gap(RightGap.SMALL)
          actionButton(CopyAction(cssHexDataKey), ColorPicker::class.java.name)
        }
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      row {
          label("").bindText(cssHexWithAlpha).gap(RightGap.SMALL)
          actionButton(CopyAction(cssHexWithAlphaDataKey), ColorPicker::class.java.name)
        }
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      row {
          label("").bindText(cssHls).gap(RightGap.SMALL)
          actionButton(CopyAction(cssHslDataKey), ColorPicker::class.java.name)
        }
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      row {
          label("").bindText(cssHlsWithAlpha).gap(RightGap.SMALL)
          actionButton(CopyAction(cssHslWithAlphaDataKey), ColorPicker::class.java.name)
        }
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      row {
          button(UiToolsBundle.message("color-picker.parse-css-color-action.title")) {
            val inputDialog =
              Messages.InputDialog(
                project,
                UiToolsBundle.message("color-picker.parse-css-color-action.input"),
                UiToolsBundle.message("color-picker.parse-css-color-action.title"),
                null,
                "",
                NotBlankInputValidator(),
              )
            inputDialog.show()
            inputDialog.inputString?.let { parseCssColorValue(it) }?.let { selectedColor.set(it) }
          }
        }
        .topGap(TopGap.NONE)
    }

    collapsibleGroup(UiToolsBundle.message("color-picker.settings.title")) {
      row {
        textField()
          .label(UiToolsBundle.message("color-picker.settings.decimal-places"))
          .bindIntTextImproved(decimalPlaces)
          .validateLongValue(LongRange(1, 4))
          .columns(COLUMNS_TINY)
          .whenTextChangedFromUi { setCssValues() }
      }
    }
  }

  override fun afterBuildUi() {
    setCssValues()
  }

  override fun getData(dataId: String): Any? =
    when {
      cssRgbDataKey.`is`(dataId) -> StringUtil.stripHtml(cssRgb.get(), false)
      cssRgbWithAlphaDataKey.`is`(dataId) -> StringUtil.stripHtml(cssRgbWithAlpha.get(), false)
      cssHexDataKey.`is`(dataId) -> StringUtil.stripHtml(cssHex.get(), false)
      cssHexWithAlphaDataKey.`is`(dataId) -> StringUtil.stripHtml(cssHexWithAlpha.get(), false)
      cssHslDataKey.`is`(dataId) -> StringUtil.stripHtml(cssHls.get(), false)
      cssHslWithAlphaDataKey.`is`(dataId) -> StringUtil.stripHtml(cssHlsWithAlpha.get(), false)
      else -> null
    }

  fun createCssValues(color: Color): CssValues {
    val red = color.red
    val green = color.green
    val blue = color.blue
    val alpha = color.alpha
    val hsl = rgbToHsl(red, green, blue)
    return CssValues(
      rgb = formatCssRgb(red = red, green = green, blue = blue),
      rgbWithAlpha = formatCssRgb(red = red, green = green, blue = blue, alpha = alpha),
      hex = "#%02X%02X%02X".format(Locale.US, red, green, blue),
      hexWithAlpha = "#%02X%02X%02X%02X".format(Locale.US, red, green, blue, alpha),
      hls = formatCssHsl(hsl),
      hlsWithAlpha = formatCssHsl(hsl, alpha),
    )
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun parseCssColorValue(inputString: String): JBColor? =
    try {
      val color = org.silentsoft.csscolor4j.Color.valueOf(inputString)
      JBColor(
        Color(color.red, color.green, color.blue, (color.opacity * 255.0).toInt()),
        Color(color.red, color.green, color.blue, (color.opacity * 255.0).toInt()),
      )
    } catch (e: IllegalArgumentException) {
      Messages.showErrorDialog(
        project,
        e.message,
        UiToolsBundle.message("color-picker.parse-css-color-action.title"),
      )
      null
    } catch (_: Exception) {
      Messages.showErrorDialog(
        project,
        UiToolsBundle.message("color-picker.parse-css-color-action.error.message"),
        UiToolsBundle.message("color-picker.parse-css-color-action.title"),
      )
      null
    }

  private fun setCssValues() {
    setCssValues(createCssValues(selectedColor.get()))
  }

  private fun setCssValues(cssValues: CssValues) {
    fun String.formatHtml(): String = "<html><code><b>${this}</b></code></html>"

    cssRgb.set(cssValues.rgb.formatHtml())
    cssRgbWithAlpha.set(cssValues.rgbWithAlpha.formatHtml())
    cssHex.set(cssValues.hex.formatHtml())
    cssHexWithAlpha.set(cssValues.hexWithAlpha.formatHtml())
    cssHls.set(cssValues.hls.formatHtml())
    cssHlsWithAlpha.set(cssValues.hlsWithAlpha.formatHtml())
  }

  private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    val rNorm = r / 255f
    val gNorm = g / 255f
    val bNorm = b / 255f

    val max = maxOf(rNorm, gNorm, bNorm)
    val min = minOf(rNorm, gNorm, bNorm)
    val l = (max + min) / 2f

    if (max == min) {
      return Triple(0f, 0f, l * 100) // achromatic
    }

    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)

    val h =
      when (max) {
        rNorm -> ((gNorm - bNorm) / d + if (gNorm < bNorm) 6 else 0)
        gNorm -> ((bNorm - rNorm) / d + 2)
        else -> ((rNorm - gNorm) / d + 4)
      } / 6f

    return Triple(h * 360, s * 100, l * 100)
  }

  private fun formatCssRgb(red: Int, green: Int, blue: Int, alpha: Int? = null): String {
    return if (alpha != null) {
      val alphaNormalized = alpha.coerceIn(0, 255) / 255f
      val aStr = formatNumber(alphaNormalized)
      "rgba($red, $green, $blue, $aStr)"
    } else {
      "rgb($red, $green, $blue)"
    }
  }

  private fun formatCssHsl(hsl: Triple<Float, Float, Float>, alpha: Int? = null): String {
    val (h, s, l) = hsl
    val hStr = formatNumber(h)
    val sStr = formatNumber(s) + "%"
    val lStr = formatNumber(l) + "%"

    return if (alpha != null) {
      val alphaNormalized = alpha.coerceIn(0, 255) / 255f
      val aStr = formatNumber(alphaNormalized)
      "hsla($hStr, $sStr, $lStr, $aStr)"
    } else {
      "hsl($hStr, $sStr, $lStr)"
    }
  }

  fun formatNumber(value: Float): String {
    val rounded = String.format(Locale.US, "%.${decimalPlaces.get()}f", value)
    return rounded.trimEnd('0').trimEnd('.')
  }

  // -- Inner Type ---------------------------------------------------------- //

  data class CssValues(
    val rgb: String,
    val rgbWithAlpha: String,
    val hex: String,
    val hexWithAlpha: String,
    val hls: String,
    val hlsWithAlpha: String,
  )

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<ColorPicker> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("color-picker.title"),
        contentTitle = UiToolsBundle.message("color-picker.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> ColorPicker) = { configuration ->
      ColorPicker(project, configuration, parentDisposable)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val COLOR_SELECTION_CHANGE_ID = "colorSelection"

    private val cssRgbDataKey = DataKey.create<String>("cssRgb")
    private val cssRgbWithAlphaDataKey = DataKey.create<String>("cssRgbWithAlpha")
    private val cssHexDataKey = DataKey.create<String>("cssHex")
    private val cssHexWithAlphaDataKey = DataKey.create<String>("cssHexWithAlpha")
    private val cssHslDataKey = DataKey.create<String>("cssHsl")
    private val cssHslWithAlphaDataKey = DataKey.create<String>("cssHslWithAlpha")
  }
}
