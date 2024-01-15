package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.not
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.ChangeListener
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.ResetListener
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.LocaleContainer.Companion.ALL_AVAILABLE_LOCALES
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.PropertyComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.isWithinLongRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.validateBigDecimalValue
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.CENTURIES
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.DAYS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.DECADES
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.HOURS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.MILLENNIUMS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.MILLISECONDS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.MINUTES
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.MONTHS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.NANOSECONDS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.SECONDS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.WEEKS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TimeConversion.ChangeOrigin.YEARS
import java.math.BigDecimal
import java.math.BigDecimal.TEN
import java.math.BigDecimal.ZERO
import java.math.MathContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParsePosition
import java.time.Duration
import java.util.*

internal class TimeConversion(
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperUiTool(parentDisposable), ResetListener, ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val nanoseconds = configuration.register("nanoseconds", ZERO, INPUT, NANOSECONDS_EXAMPLE)
  private val milliseconds = configuration.register("milliseconds", ZERO, INPUT)
  private val seconds = configuration.register("seconds", ZERO, INPUT)
  private val minutes = configuration.register("minutes", ZERO, INPUT)
  private val hours = configuration.register("hours", ZERO, INPUT)
  private val days = configuration.register("days", ZERO, INPUT)
  private val weeks = configuration.register("weeks", ZERO, INPUT)
  private val months = configuration.register("months", ZERO, INPUT)
  private val years = configuration.register("years", ZERO, INPUT)
  private val decades = configuration.register("decades", ZERO, INPUT)
  private val centuries = configuration.register("centuries", ZERO, INPUT)
  private val millenniums = configuration.register("millenniums", ZERO, INPUT)

  private var parsingLocale = configuration.register("parsingLocale", DEFAULT_PARSING_LOCALE)
  private val roundingMode = configuration.register("roundingMode", DEFAULT_ROUNDING_MODE, CONFIGURATION)
  private val decimalPlaces = configuration.register("decimalPlaces", DECIMAL_PLACES, CONFIGURATION)

  private val nanosecondsFormatted = ValueProperty("0")
  private val millisecondsFormatted = ValueProperty("0")
  private val secondsFormatted = ValueProperty("0")
  private val minutesFormatted = ValueProperty("0")
  private val hoursFormatted = ValueProperty("0")
  private val daysFormatted = ValueProperty("0")
  private val weeksFormatted = ValueProperty("0")
  private val monthsFormatted = ValueProperty("0")
  private val yearsFormatted = ValueProperty("0")
  private val decadesFormatted = ValueProperty("0")
  private val centuriesFormatted = ValueProperty("0")
  private val millenniumsFormatted = ValueProperty("0")
  private val minutesDetail = ValueProperty("0")
  private val hoursDetail = ValueProperty("0")
  private val daysDetail = ValueProperty("0")
  private val parsingDecimalSeparatorInfo = ValueProperty("")

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    if (!ALL_AVAILABLE_LOCALES.contains(parsingLocale.get())) {
      parsingLocale.set(DEFAULT_PARSING_LOCALE)
    }
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun activated() {
    configuration.addChangeListener(parentDisposable, this)
    configuration.addResetListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
    configuration.removeResetListener(this)
  }

  override fun configurationChanged(property: ValueProperty<out Any>) {
    if (property == roundingMode || property == decimalPlaces || property == parsingLocale) {
      sync()
    }
  }

  override fun configurationReset() {
    sync()
  }

  override fun afterBuildUi() {
    sync()
  }

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Nanoseconds:")
        .bindText(nanosecondsFormatted)
        .whenTextChangedFromUi { convert(NANOSECONDS) }
        .columns(15)
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Milliseconds:")
        .bindText(millisecondsFormatted)
        .whenTextChangedFromUi { convert(MILLISECONDS) }
        .columns(15)
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Seconds:")
        .bindText(secondsFormatted)
        .whenTextChangedFromUi { convert(SECONDS) }
        .columns(15)
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Minutes:")
        .bindText(minutesFormatted)
        .whenTextChangedFromUi { convert(MINUTES) }
        .columns(15)
        .gap(RightGap.SMALL)
      comment("")
        .bindText(minutesDetail)
        .visibleIf(PropertyComponentPredicate(minutesDetail, "").not())
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Hours:")
        .bindText(hoursFormatted)
        .whenTextChangedFromUi { convert(HOURS) }
        .columns(15)
        .gap(RightGap.SMALL)
      comment("")
        .bindText(hoursDetail)
        .visibleIf(PropertyComponentPredicate(hoursDetail, "").not())
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Days:")
        .comment("One month is equal to 30.416 days.")
        .bindText(daysFormatted)
        .whenTextChangedFromUi { convert(DAYS) }
        .columns(15)
        .gap(RightGap.SMALL)
      comment("")
        .bindText(daysDetail)
        .visibleIf(PropertyComponentPredicate(daysDetail, "").not())
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Months:")
        .bindText(monthsFormatted)
        .whenTextChangedFromUi { convert(MONTHS) }
        .columns(15)
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Years:")
        .comment("One year is equal to 365 days.")
        .bindText(yearsFormatted)
        .whenTextChangedFromUi { convert(YEARS) }
        .columns(15)
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Decades:")
        .bindText(decadesFormatted)
        .whenTextChangedFromUi { convert(DECADES) }
        .columns(15)
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Centuries:")
        .bindText(centuriesFormatted)
        .whenTextChangedFromUi { convert(CENTURIES) }
        .columns(15)
    }.layout(RowLayout.PARENT_GRID)
    row {
      textField()
        .validateBigDecimalValue(ZERO, FIXED_MATH_CONTEXT) { it.parseBigDecimal(getMathContext()) }
        .label("Millenniums:")
        .bindText(millenniumsFormatted)
        .whenTextChangedFromUi { convert(MILLENNIUMS) }
        .columns(15)
    }.layout(RowLayout.PARENT_GRID)

    group("Settings") {
      row {
        comboBox(ALL_AVAILABLE_LOCALES)
          .label("Locale for parsing:")
          .bindItem(parsingLocale)
          .columns(COLUMNS_MEDIUM)
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
      }.layout(RowLayout.PARENT_GRID)
      row {
        comboBox(RoundingMode.entries)
          .label("Rounding mode:")
          .bindItem(roundingMode)
      }.layout(RowLayout.PARENT_GRID)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun convert(changeOrigin: ChangeOrigin? = null, changeOriginAsNanoseconds: BigDecimal? = null) {
    try {
      val mathContext = getMathContext()
      val monthToNanoseconds = BigDecimal.valueOf(365L).divide(BigDecimal.valueOf(12L), mathContext).multiply(DAYS_TO_NANOSECONDS, mathContext)
      val millenniumsToNanoseconds = CENTURIES_TO_NANOSECONDS.multiply(TEN, mathContext)

      val changeOriginAsNanosecondsToUse: BigDecimal? = when (changeOrigin) {
        NANOSECONDS -> nanosecondsFormatted.get().parseBigDecimal(mathContext)
        MILLISECONDS -> millisecondsFormatted.get().parseBigDecimal(mathContext).multiply(MILLISECONDS_TO_NANOSECONDS, mathContext)
        SECONDS -> secondsFormatted.get().parseBigDecimal(mathContext).multiply(SECONDS_TO_NANOSECONDS, mathContext)
        MINUTES -> minutesFormatted.get().parseBigDecimal(mathContext).multiply(MINUTES_TO_NANOSECONDS, mathContext)
        HOURS -> hoursFormatted.get().parseBigDecimal(mathContext).multiply(HOURS_TO_NANOSECONDS, mathContext)
        DAYS -> daysFormatted.get().parseBigDecimal(mathContext).multiply(DAYS_TO_NANOSECONDS, mathContext)
        WEEKS -> weeksFormatted.get().parseBigDecimal(mathContext).multiply(WEEK_TO_NANOSECONDS, mathContext)
        MONTHS -> monthsFormatted.get().parseBigDecimal(mathContext).multiply(monthToNanoseconds, mathContext)
        YEARS -> yearsFormatted.get().parseBigDecimal(mathContext).multiply(YEARS_TO_NANOSECONDS, mathContext)
        DECADES -> decadesFormatted.get().parseBigDecimal(mathContext).multiply(DECADES_TO_NANOSECONDS, mathContext)
        CENTURIES -> centuriesFormatted.get().parseBigDecimal(mathContext).multiply(CENTURIES_TO_NANOSECONDS, mathContext)
        MILLENNIUMS -> millenniumsFormatted.get().parseBigDecimal(mathContext).multiply(millenniumsToNanoseconds, mathContext)
        else -> changeOriginAsNanoseconds!!
      }
      if (changeOriginAsNanosecondsToUse == null) {
        return
      }

      nanoseconds.set(changeOriginAsNanosecondsToUse)
      milliseconds.set(changeOriginAsNanosecondsToUse.divide(MILLISECONDS_TO_NANOSECONDS, mathContext))
      seconds.set(changeOriginAsNanosecondsToUse.divide(SECONDS_TO_NANOSECONDS, mathContext))
      minutes.set(changeOriginAsNanosecondsToUse.divide(MINUTES_TO_NANOSECONDS, mathContext))
      hours.set(changeOriginAsNanosecondsToUse.divide(HOURS_TO_NANOSECONDS, mathContext))
      days.set(changeOriginAsNanosecondsToUse.divide(DAYS_TO_NANOSECONDS, mathContext))
      months.set(changeOriginAsNanosecondsToUse.divide(monthToNanoseconds, mathContext))
      weeks.set(changeOriginAsNanosecondsToUse.divide(WEEK_TO_NANOSECONDS, mathContext))
      years.set(changeOriginAsNanosecondsToUse.divide(YEARS_TO_NANOSECONDS, mathContext))
      decades.set(changeOriginAsNanosecondsToUse.divide(DECADES_TO_NANOSECONDS, mathContext))
      centuries.set(changeOriginAsNanosecondsToUse.divide(CENTURIES_TO_NANOSECONDS, mathContext))
      millenniums.set(changeOriginAsNanosecondsToUse.divide(millenniumsToNanoseconds, mathContext))

      if (changeOrigin != NANOSECONDS) {
        nanosecondsFormatted.set(nanoseconds.get().formatted())
      }
      if (changeOrigin != MILLISECONDS) {
        millisecondsFormatted.set(milliseconds.get().formatted())
      }
      if (changeOrigin != SECONDS) {
        secondsFormatted.set(seconds.get().formatted())
      }
      if (changeOrigin != MINUTES) {
        minutesFormatted.set(minutes.get().formatted())
      }
      if (changeOrigin != HOURS) {
        hoursFormatted.set(hours.get().formatted())
      }
      if (changeOrigin != DAYS) {
        daysFormatted.set(days.get().formatted())
      }
      if (changeOrigin != MONTHS) {
        monthsFormatted.set(months.get().formatted())
      }
      if (changeOrigin != WEEKS) {
        weeksFormatted.set(weeks.get().formatted())
      }
      if (changeOrigin != YEARS) {
        yearsFormatted.set(years.get().formatted())
      }
      if (changeOrigin != DECADES) {
        decadesFormatted.set(decades.get().formatted())
      }
      if (changeOrigin != CENTURIES) {
        centuriesFormatted.set(centuries.get().formatted())
      }
      if (changeOrigin != MILLENNIUMS) {
        millenniumsFormatted.set(millenniums.get().formatted())
      }

      formatDetails()
    } catch (e: Exception) {
      log.warn("Failed to convert time", e)
    }
  }

  private fun BigDecimal.formatted() = this.stripTrailingZeros()
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

  private fun formatDetails() {
    try {
      val javaMathRoundingMode = roundingMode.get().javaMathRoundingMode
      val mathContext = MathContext(decimalPlaces.get(), javaMathRoundingMode)

      run {
        val daysAsNanos = days.get().multiply(DAYS_TO_NANOSECONDS, mathContext).setScale(0, javaMathRoundingMode)
        if (daysAsNanos.isWithinLongRange()) {
          val duration = Duration.ofNanos(daysAsNanos.longValueExact())
          daysDetail.set(
            buildString {
              append("${DETAILS_FORMAT.format(duration.toDays())}d ")
              append("${DETAILS_FORMAT.format(duration.toHoursPart())}h ")
              append("${DETAILS_FORMAT.format(duration.toMinutesPart())}m ")
              append("${DETAILS_FORMAT.format(duration.toSecondsPart())}s")
            }
          )
        }
        else {
          daysDetail.set("")
        }
      }

      run {
        val hoursAsNanos = hours.get().multiply(HOURS_TO_NANOSECONDS, mathContext).setScale(0, javaMathRoundingMode)
        if (hoursAsNanos.isWithinLongRange()) {
          val duration = Duration.ofNanos(hoursAsNanos.longValueExact())
          hoursDetail.set(
            buildString {
              append("${DETAILS_FORMAT.format(duration.toHours())}h ")
              append("${DETAILS_FORMAT.format(duration.toMinutesPart())}m ")
              append("${DETAILS_FORMAT.format(duration.toSecondsPart())}s")
            }
          )
        }
        else {
          hoursDetail.set("")
        }
      }

      run {
        val minutesAsNanos = minutes.get().multiply(MINUTES_TO_NANOSECONDS, mathContext).setScale(0, javaMathRoundingMode)
        if (minutesAsNanos.isWithinLongRange()) {
          val duration = Duration.ofNanos(minutesAsNanos.longValueExact())
          minutesDetail.set(
            buildString {
              append("${DETAILS_FORMAT.format(duration.toMinutes())}m ")
              append("${DETAILS_FORMAT.format(duration.toSecondsPart())}s")
            }
          )
        }
        else {
          minutesDetail.set("")
        }
      }
    } catch (e: Exception) {
      log.warn("Failed to format time conversion details", e)
      daysDetail.set("")
      hoursDetail.set("")
      minutesDetail.set("")
    }
  }

  private fun String.parseBigDecimal(mathContext: MathContext): BigDecimal {
    val decimalFormat = DecimalFormat().apply {
      decimalFormatSymbols = DecimalFormatSymbols.getInstance(parsingLocale.get().locale)
    }
    // The `DecimalFormat` is non-strict. So `12foo3` would return `123`.
    val parsePosition = ParsePosition(0)
    val parsedNumber = decimalFormat.parse(this, parsePosition)
    if (parsePosition.index != this.length) {
      throw NumberFormatException("Invalid number format: $this")
    }
    return BigDecimal(parsedNumber.toString(), mathContext)
  }

  private fun getMathContext() = MathContext(decimalPlaces.get() + 1, roundingMode.get().javaMathRoundingMode)

  private fun getDecimalSeparator(): String = DecimalFormatSymbols.getInstance(parsingLocale.get().locale).decimalSeparator.toString()

  private fun sync() {
    convert(null, nanoseconds.get())
    formatDetails()

    val decimalSeparator = getDecimalSeparator()
    val postfix = when (decimalSeparator) {
      "." -> " (dot)"
      "," -> " (comma)"
      else -> ""
    }
    parsingDecimalSeparatorInfo.set("<html>Decimal separator: <b><code>$decimalSeparator</code></b>$postfix</html>")
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class ChangeOrigin {

    NANOSECONDS,
    MILLISECONDS,
    SECONDS,
    MINUTES,
    HOURS,
    DAYS,
    WEEKS,
    MONTHS,
    YEARS,
    DECADES,
    CENTURIES,
    MILLENNIUMS
  }

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

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TimeConversion> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Time Conversion",
      contentTitle = "Time Conversion"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> TimeConversion) =
      { configuration -> TimeConversion(configuration, parentDisposable) }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val log = logger<TimeConversion>()

    private val DEFAULT_PARSING_LOCALE = LocaleContainer(Locale.getDefault())

    private val NANOSECONDS_EXAMPLE = BigDecimal.valueOf(123460000000000)
    private const val DECIMAL_PLACES = 5
    private val DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP

    private val DETAILS_FORMAT = DecimalFormat("0")
    private val FIXED_MATH_CONTEXT = MathContext(10, java.math.RoundingMode.HALF_UP)

    private val MILLISECONDS_TO_NANOSECONDS = BigDecimal.valueOf(Duration.ofMillis(1).toNanos())
    private val SECONDS_TO_NANOSECONDS = BigDecimal.valueOf(Duration.ofSeconds(1).toNanos())
    private val MINUTES_TO_NANOSECONDS = BigDecimal.valueOf(Duration.ofMinutes(1).toNanos())
    private val HOURS_TO_NANOSECONDS = BigDecimal.valueOf(Duration.ofHours(1).toNanos())
    private val DAYS_TO_NANOSECONDS = BigDecimal.valueOf(Duration.ofDays(1).toNanos())
    private val WEEK_TO_NANOSECONDS = BigDecimal.valueOf(Duration.ofDays(7).toNanos())
    private val YEARS_TO_NANOSECONDS = BigDecimal.valueOf(Duration.ofDays(365).toNanos())
    private val DECADES_TO_NANOSECONDS = BigDecimal.valueOf(Duration.ofDays(365 * 10).toNanos())
    private val CENTURIES_TO_NANOSECONDS = BigDecimal.valueOf(Duration.ofDays(365 * 100).toNanos())
  }
}