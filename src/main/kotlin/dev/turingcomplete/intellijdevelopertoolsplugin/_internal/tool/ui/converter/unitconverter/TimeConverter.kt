package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.not
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.PropertyComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.isWithinLongRange
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.validateBigDecimalValue
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.CENTURIES
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.DAYS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.DECADES
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.HOURS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.MILLENNIUMS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.MILLISECONDS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.MINUTES
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.MONTHS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.NANOSECONDS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.SECONDS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.WEEKS
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.TimeConverter.ChangeOrigin.YEARS
import java.math.BigDecimal
import java.math.BigDecimal.TEN
import java.math.BigDecimal.ZERO
import java.math.MathContext
import java.text.DecimalFormat
import java.time.Duration
import javax.swing.JComponent

internal class TimeConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : UnitConverter(CONFIGURATION_KEY_PREFIX, configuration, parentDisposable, "Time") {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val nanoseconds = configuration.register("${CONFIGURATION_KEY_PREFIX}timeNanoseconds", ZERO, INPUT, NANOSECONDS_EXAMPLE)

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

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    data class TimeField(
      val title: String,
      val valueProperty: ValueProperty<String>,
      val changeOrigin: ChangeOrigin,
      val contextHelp: String? = null,
      val detail: ValueProperty<String>? = null
    )

    listOf(
      TimeField("Nanoseconds", nanosecondsFormatted, NANOSECONDS),
      TimeField("Milliseconds", millisecondsFormatted, MILLISECONDS),
      TimeField("Seconds", secondsFormatted, SECONDS),
      TimeField("Minutes", minutesFormatted, MINUTES, null, minutesDetail),
      TimeField("Hours", hoursFormatted, HOURS, null, hoursDetail),
      TimeField("Days", daysFormatted, DAYS, null, daysDetail),
      TimeField("Months", monthsFormatted, MONTHS, "One month is equal to 30.416 days (365/12)."),
      TimeField("Years", yearsFormatted, YEARS),
      TimeField("Decades", decadesFormatted, DECADES),
      TimeField("Centuries", centuriesFormatted, CENTURIES),
      TimeField("Millenniums", millenniumsFormatted, MILLENNIUMS),
    ).forEach { (title, valueProperty, changeOrigin, contextHelp, detail) ->
      row {
        lateinit var textField: JBTextField
        textField = textField()
          .validateBigDecimalValue(ZERO, mathContext) { it.parseBigDecimal() }
          .label("$title:")
          .bindText(valueProperty)
          .whenTextChangedFromUi { convert(changeOrigin, textField) }
          .columns(15)
          .component
        if (detail != null) {
          comment("")
            .bindText(detail)
            .visibleIf(PropertyComponentPredicate(detail, "").not())
        }
        if (contextHelp != null) {
          contextHelp(contextHelp)
        }
      }.layout(RowLayout.PARENT_GRID)
    }
  }

  override fun doSync() {
    convert(null, null, nanoseconds.get())
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun convert(
    changeOrigin: ChangeOrigin? = null,
    changeOriginComponent: JComponent? = null,
    changeOriginAsNanoseconds: BigDecimal? = null
  ) {
    if (changeOriginComponent != null && validate().any { it.component == changeOriginComponent }) {
      return
    }

    try {
      val monthToNanoseconds = BigDecimal.valueOf(365L).divide(BigDecimal.valueOf(12L), mathContext).multiply(
        DAYS_TO_NANOSECONDS, mathContext)
      val millenniumsToNanoseconds = CENTURIES_TO_NANOSECONDS.multiply(TEN, mathContext)

      val changeOriginAsNanosecondsToUse: BigDecimal? = when (changeOrigin) {
        NANOSECONDS -> nanosecondsFormatted.get().parseBigDecimal()
        MILLISECONDS -> millisecondsFormatted.get().parseBigDecimal().multiply(MILLISECONDS_TO_NANOSECONDS, mathContext)
        SECONDS -> secondsFormatted.get().parseBigDecimal().multiply(SECONDS_TO_NANOSECONDS, mathContext)
        MINUTES -> minutesFormatted.get().parseBigDecimal().multiply(MINUTES_TO_NANOSECONDS, mathContext)
        HOURS -> hoursFormatted.get().parseBigDecimal().multiply(HOURS_TO_NANOSECONDS, mathContext)
        DAYS -> daysFormatted.get().parseBigDecimal().multiply(DAYS_TO_NANOSECONDS, mathContext)
        WEEKS -> weeksFormatted.get().parseBigDecimal().multiply(WEEK_TO_NANOSECONDS, mathContext)
        MONTHS -> monthsFormatted.get().parseBigDecimal().multiply(monthToNanoseconds, mathContext)
        YEARS -> yearsFormatted.get().parseBigDecimal().multiply(YEARS_TO_NANOSECONDS, mathContext)
        DECADES -> decadesFormatted.get().parseBigDecimal().multiply(DECADES_TO_NANOSECONDS, mathContext)
        CENTURIES -> centuriesFormatted.get().parseBigDecimal().multiply(CENTURIES_TO_NANOSECONDS, mathContext)
        MILLENNIUMS -> millenniumsFormatted.get().parseBigDecimal().multiply(millenniumsToNanoseconds, mathContext)
        else -> changeOriginAsNanoseconds!!
      }
      if (changeOriginAsNanosecondsToUse == null) {
        return
      }

      nanoseconds.set(changeOriginAsNanosecondsToUse)
      val minutes = changeOriginAsNanosecondsToUse.divide(MINUTES_TO_NANOSECONDS, mathContext)
      val hours = changeOriginAsNanosecondsToUse.divide(HOURS_TO_NANOSECONDS, mathContext)
      val days = changeOriginAsNanosecondsToUse.divide(DAYS_TO_NANOSECONDS, mathContext)

      if (changeOrigin != NANOSECONDS) {
        nanosecondsFormatted.set(nanoseconds.get().toFormatted())
      }
      if (changeOrigin != MILLISECONDS) {
        val milliseconds = changeOriginAsNanosecondsToUse.divide(MILLISECONDS_TO_NANOSECONDS, mathContext)
        millisecondsFormatted.set(milliseconds.toFormatted())
      }
      if (changeOrigin != SECONDS) {
        val seconds = changeOriginAsNanosecondsToUse.divide(SECONDS_TO_NANOSECONDS, mathContext)
        secondsFormatted.set(seconds.toFormatted())
      }
      if (changeOrigin != MINUTES) {
        minutesFormatted.set(minutes.toFormatted())
      }
      if (changeOrigin != HOURS) {
        hoursFormatted.set(hours.toFormatted())
      }
      if (changeOrigin != DAYS) {
        daysFormatted.set(days.toFormatted())
      }
      if (changeOrigin != MONTHS) {
        val months = changeOriginAsNanosecondsToUse.divide(monthToNanoseconds, mathContext)
        monthsFormatted.set(months.toFormatted())
      }
      if (changeOrigin != WEEKS) {
        val weeks = changeOriginAsNanosecondsToUse.divide(WEEK_TO_NANOSECONDS, mathContext)
        weeksFormatted.set(weeks.toFormatted())
      }
      if (changeOrigin != YEARS) {
        val years = changeOriginAsNanosecondsToUse.divide(YEARS_TO_NANOSECONDS, mathContext)
        yearsFormatted.set(years.toFormatted())
      }
      if (changeOrigin != DECADES) {
        val decades = changeOriginAsNanosecondsToUse.divide(DECADES_TO_NANOSECONDS, mathContext)
        decadesFormatted.set(decades.toFormatted())
      }
      if (changeOrigin != CENTURIES) {
        val centuries = changeOriginAsNanosecondsToUse.divide(CENTURIES_TO_NANOSECONDS, mathContext)
        centuriesFormatted.set(centuries.toFormatted())
      }
      if (changeOrigin != MILLENNIUMS) {
        val millenniums = changeOriginAsNanosecondsToUse.divide(millenniumsToNanoseconds, mathContext)
        millenniumsFormatted.set(millenniums.toFormatted())
      }

      formatDetails(days = days, hours = hours, minutes = minutes)
    } catch (e: Exception) {
      log.warn("Failed to convert time", e)
    }
  }

  private fun formatDetails(days: BigDecimal, hours: BigDecimal, minutes: BigDecimal) {
    try {
      run {
        val daysAsNanos = days.multiply(DAYS_TO_NANOSECONDS, mathContext).setScale(0, mathContext.roundingMode)
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
        val hoursAsNanos = hours.multiply(HOURS_TO_NANOSECONDS, mathContext).setScale(0, mathContext.roundingMode)
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
        val minutesAsNanos = minutes.multiply(MINUTES_TO_NANOSECONDS, mathContext).setScale(0, mathContext.roundingMode)
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
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val log = logger<TimeConverter>()

    private const val CONFIGURATION_KEY_PREFIX = "timeConverter_"

    private val NANOSECONDS_EXAMPLE = BigDecimal.valueOf(123460000000000)

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