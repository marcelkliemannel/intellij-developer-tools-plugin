package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter

import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.LocaleContainer.Companion.ALL_AVAILABLE_LOCALES
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParsePosition
import java.util.*

abstract class MathContextUnitConverter(
  configurationKeyPrefix: String,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  title: String
): UnitConverter(parentDisposable, title) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var parsingLocale = configuration.register("${configurationKeyPrefix}parsingLocale", DEFAULT_PARSING_LOCALE)
  private val roundingMode = configuration.register("${configurationKeyPrefix}roundingMode", DEFAULT_ROUNDING_MODE)
  private val decimalPlaces = configuration.register("${configurationKeyPrefix}decimalPlaces", DEFAULT_DECIMAL_PLACES)
  private val precision = configuration.register("${configurationKeyPrefix}precision", DEFAULT_PRECISION)

  private val parsingDecimalSeparatorInfo = ValueProperty("")
  var mathContext = createMathContext()
    private set

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  open fun Panel.buildAdditionalSettingsUi() {
    // Override if needed
  }

  fun String.parseBigDecimal(): BigDecimal {
    val decimalFormat = DecimalFormat().apply {
      decimalFormatSymbols = DecimalFormatSymbols.getInstance(parsingLocale.get().locale)
    }
    // The `DecimalFormat` is non-strict. So `12foo3` would return `123`.
    val parsePosition = ParsePosition(0)
    val parsedNumber: Number? = decimalFormat.parse(this, parsePosition)
    if (parsePosition.index != this.length || parsedNumber == null) {
      throw NumberFormatException("Invalid number format: $this")
    }
    return BigDecimal(parsedNumber.toString(), mathContext)
  }

  fun BigDecimal.toFormatted() = this.stripTrailingZeros()
    .setScale(decimalPlaces.get(), roundingMode.get().javaMathRoundingMode)
    .toPlainString()
    .let {
      // Remove trailing zeros
      if (it.contains(".")) {
        val trimmed = it.trimEnd('0')
        if (trimmed.endsWith('.')) trimmed.dropLast(1) else trimmed
      }
      else {
        it
      }
    }.replace(".", getDecimalSeparator())

  override fun sync() {
    mathContext = createMathContext()

    val decimalSeparator = getDecimalSeparator()
    val postfix = when (decimalSeparator) {
      "." -> " (dot)"
      "," -> " (comma)"
      else -> ""
    }
    parsingDecimalSeparatorInfo.set("<html>Decimal separator: <b><code>$decimalSeparator</code></b>$postfix</html>")

    doSync()
  }

  abstract fun doSync()

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun getDecimalSeparator(): String =
    DecimalFormatSymbols.getInstance(parsingLocale.get().locale).decimalSeparator.toString()

  @Suppress("UnstableApiUsage")
  override fun Panel.buildSettingsUi() {
    collapsibleGroup("Settings") {
      buildAdditionalSettingsUi()
      row {
        comboBox(ALL_AVAILABLE_LOCALES)
          .label("Locale for parsing:")
          .bindItem(parsingLocale)
          .columns(COLUMNS_MEDIUM)
          .whenItemSelectedFromUi { sync() }
      }.layout(RowLayout.PARENT_GRID).bottomGap(BottomGap.NONE)
      row {
        cell()
        label("")
          .bindText(parsingDecimalSeparatorInfo)
      }.layout(RowLayout.PARENT_GRID).topGap(TopGap.NONE)
      row {
        textField()
          .label("Decimal places:")
          .bindIntTextImproved(decimalPlaces)
          .validateLongValue(LongRange(1, 50))
          .columns(COLUMNS_TINY)
          .whenTextChangedFromUi { sync() }
      }.layout(RowLayout.PARENT_GRID)
      row {
        comboBox(RoundingMode.entries)
          .label("Rounding mode:")
          .bindItem(roundingMode)
          .whenItemSelectedFromUi { sync() }
      }.layout(RowLayout.PARENT_GRID)
      row {
        textField()
          .label("Precision:")
          .bindIntTextImproved(precision)
          .validateLongValue(LongRange(1, 100))
          .columns(COLUMNS_TINY)
          .whenTextChangedFromUi { sync() }
      }.layout(RowLayout.PARENT_GRID)
    }.topGap(TopGap.NONE)
  }

  private fun createMathContext() =
    MathContext(precision.get(), roundingMode.get().javaMathRoundingMode)

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class RoundingMode(val title: String, val javaMathRoundingMode: java.math.RoundingMode) {

    DOWN("Down", java.math.RoundingMode.DOWN),
    UP("Up", java.math.RoundingMode.UP),
    CEILING("Ceiling", java.math.RoundingMode.CEILING),
    FLOOR("Floor", java.math.RoundingMode.FLOOR),
    HALF_UP("Half up", java.math.RoundingMode.HALF_UP),
    HALF_DOWN("Half down", java.math.RoundingMode.HALF_DOWN),
    HALF_EVEN("Half even", java.math.RoundingMode.HALF_EVEN);

    override fun toString(): String = title
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP
    private const val DEFAULT_DECIMAL_PLACES = 5
    private const val DEFAULT_PRECISION = 50
    private val DEFAULT_PARSING_LOCALE = LocaleContainer(Locale.getDefault())
  }
}