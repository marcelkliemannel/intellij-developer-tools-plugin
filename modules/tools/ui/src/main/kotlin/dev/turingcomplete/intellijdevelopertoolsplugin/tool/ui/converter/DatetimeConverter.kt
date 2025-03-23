package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil.stripHtml
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.text
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.ComboBoxPredicate
import com.intellij.util.Alarm
import com.intellij.util.text.OrdinalFormat.formatEnglish
import dev.turingcomplete.intellijdevelopertoolsplugin.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugin.common.LocaleContainer.Companion.ALL_AVAILABLE_LOCALES
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin.common.bindLongTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin.common.changeFont
import dev.turingcomplete.intellijdevelopertoolsplugin.common.not
import dev.turingcomplete.intellijdevelopertoolsplugin.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.CopyAction
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.DatetimeConverter.ConversionOrigin.DAY
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.DatetimeConverter.ConversionOrigin.HOUR
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.DatetimeConverter.ConversionOrigin.MINUTE
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.DatetimeConverter.ConversionOrigin.MONTH
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.DatetimeConverter.ConversionOrigin.SECOND
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.DatetimeConverter.ConversionOrigin.TIME_ZONE
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.DatetimeConverter.ConversionOrigin.UNIX_TIMESTAMP_MILLIS
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.DatetimeConverter.ConversionOrigin.UNIX_TIMESTAMP_SECONDS
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.DatetimeConverter.ConversionOrigin.YEAR
import java.awt.Font
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle.FULL_STANDALONE
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields
import java.util.*

class DatetimeConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  private val context: DeveloperUiToolContext
) : DeveloperUiTool(parentDisposable) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedTimeZoneId = configuration.register("timeZoneId", ZoneId.systemDefault().id)
  private var formattedStandardFormat = configuration.register("formattedStandardFormat", DEFAULT_FORMATTED_STANDARD_FORMAT)
  private var formattedStandardFormatAddOffset = configuration.register("formattedStandardFormatAddOffset", DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_OFFSET)
  private var formattedStandardFormatAddTimeZone = configuration.register("formattedStandardFormatAddTimeZone", DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_TIME_ZONE)
  private var formattedIndividual = configuration.register("formattedIndividual", DEFAULT_FORMATTED_INDIVIDUAL)
  private var formattedLocale = configuration.register("formattedLocale", DEFAULT_FORMATTED_LOCALE)
  private var formattedIndividualFormat = configuration.register("formattedIndividualFormat", DEFAULT_INDIVIDUAL_FORMAT)

  private var formattedStandardFormatPattern = ValueProperty("")
  private var formattedText = ValueProperty("No result")
  private var dateDetails = ValueProperty("")

  private val currentUnixTimestampUpdateAlarm by lazy { Alarm(parentDisposable) }
  private val currentUnixTimestampUpdate: Runnable by lazy { createCurrentUnixTimestampUpdate() }
  private val currentUnixTimestampSeconds = ValueProperty(System.currentTimeMillis().div(1000).toString())
  private val currentUnixTimestampMillis = ValueProperty(System.currentTimeMillis().toString())

  private val convertAlarm by lazy { Alarm(parentDisposable) }

  private val convertUnixTimeStampSeconds = ValueProperty<Long>(0)
  private val convertUnixTimeStampMillis = ValueProperty<Long>(0)
  private val convertDay =  ValueProperty(0)
  private val convertMonth = ValueProperty(0)
  private val convertYear = ValueProperty(0)
  private val convertHour = ValueProperty(0)
  private val convertMinute = ValueProperty(0)
  private val convertSecond = ValueProperty(0)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    // Validate if selected time zone and formatted local  is still available
    if (!ZoneId.getAvailableZoneIds().contains(selectedTimeZoneId.get())) {
      selectedTimeZoneId.set(ZoneId.systemDefault().id)
    }
    if (!ALL_AVAILABLE_LOCALES.contains(formattedLocale.get())) {
      formattedLocale.set(DEFAULT_FORMATTED_LOCALE)
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    group("Current Unix Timestamp") {
      if (context.prioritizeVerticalLayout) {
        row {
          buildTimestampLabelUi("Seconds:", currentUnixTimestampSeconds, TIMESTAMP_SECONDS_CONTENT_DATA_KEY)
        }
        row {
          buildTimestampLabelUi("Milliseconds:", currentUnixTimestampMillis, TIMESTAMP_MILLIS_CONTENT_DATA_KEY)
        }
      }
      else {
        row {
          buildTimestampLabelUi("Seconds:", currentUnixTimestampSeconds, TIMESTAMP_SECONDS_CONTENT_DATA_KEY)
          buildTimestampLabelUi("Milliseconds:", currentUnixTimestampMillis, TIMESTAMP_MILLIS_CONTENT_DATA_KEY)
        }
      }
    }

    group("Convert") {
      val initialInstant = Instant.ofEpochMilli(System.currentTimeMillis())
      val initialLocalDateTime = LocalDateTime.ofInstant(initialInstant, selectedTimeZoneId())

      group("Unix Timestamp") {
        if (context.prioritizeVerticalLayout) {
          row {
            buildUnixTimeStampSecondsTextFieldUi(initialInstant)
          }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE).layout(RowLayout.PARENT_GRID)
          row {
            buildUnixTimeStampMillisTextFieldUi(initialInstant)
          }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE).layout(RowLayout.PARENT_GRID)
          row {
            buildSetToNowButtonUi()
          }
        }
        else {
          row {
            buildUnixTimeStampSecondsTextFieldUi(initialInstant)
            buildUnixTimeStampMillisTextFieldUi(initialInstant)
            buildSetToNowButtonUi()
          }
        }
      }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      group("Date and Time") {
        row {
          comboBox(ZoneId.getAvailableZoneIds().sorted())
            .label("Time zone:")
            .bindItem(selectedTimeZoneId)
            .whenItemSelectedFromUi { convert(TIME_ZONE) }
            .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, TIME_ZONE) }
        }
        data class DateField(
          val title: String,
          val initialValue: Int,
          val valueProperty: ValueProperty<Int>,
          val range: LongRange,
          val changeOrigin: ConversionOrigin
        )
        row {
          listOf(
            DateField("Year", initialLocalDateTime.year, convertYear, LongRange(1970, 9999), YEAR),
            DateField("Month", initialLocalDateTime.monthValue, convertMonth, LongRange(1, 12), MONTH),
            DateField("Day", initialLocalDateTime.dayOfMonth, convertDay, LongRange(1, 31), DAY)
          ).forEach { (title, initialValue, valueProperty, range, changeOrigin) ->
            textField().label("$title:")
              .text(initialValue.toString())
              .bindIntTextImproved(valueProperty)
              .columns(5)
              .validateLongValue(range)
              .whenTextChangedFromUi { convert(changeOrigin) }
              .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, changeOrigin) }
          }
        }.layout(RowLayout.PARENT_GRID)
        row {
          listOf(
            DateField("Hour", initialLocalDateTime.hour, convertHour, LongRange(0, 23), HOUR),
            DateField("Minute", initialLocalDateTime.minute, convertMinute, LongRange(0, 59), MINUTE),
            DateField("Second", initialLocalDateTime.second, convertSecond, LongRange(0, 59), SECOND)
          ).forEach { (title, initialValue, valueProperty, range, changeOrigin) ->
            textField().label("$title:")
              .text(initialValue.toString())
              .bindIntTextImproved(valueProperty)
              .columns(5)
              .validateLongValue(range)
              .whenTextChangedFromUi { convert(changeOrigin) }
              .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, changeOrigin) }
          }
        }.layout(RowLayout.PARENT_GRID)
        row {
          comment("")
            .bindText(dateDetails)
        }
      }.layout(RowLayout.PARENT_GRID).topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      group("Formatted") {
        buttonsGroup {
          lateinit var formattedStandardFormatComboBox: ComboBox<StandardFormat>
          row {
            radioButton("Standard format:")
              .bindSelected(formattedIndividual.not())
              .onChanged { convert(UNIX_TIMESTAMP_MILLIS) }
              .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, UNIX_TIMESTAMP_MILLIS) }
              .gap(RightGap.SMALL)
            formattedStandardFormatComboBox = comboBox(StandardFormat.entries)
              .bindItem(formattedStandardFormat)
              .columns(COLUMNS_MEDIUM)
              .whenItemSelectedFromUi { syncFormattedStandardFormatPattern(); convert(UNIX_TIMESTAMP_MILLIS) }
              .enabledIf(formattedIndividual.not())
              .gap(RightGap.SMALL)
              .component
              .apply { putUserData(CONVERSION_ORIGIN_KEY, UNIX_TIMESTAMP_MILLIS) }
            if (!context.prioritizeVerticalLayout) {
              buildStandardFormatConfigurationUi(formattedStandardFormatComboBox)
            }
          }.layout(RowLayout.PARENT_GRID).bottomGap(BottomGap.NONE)
          if (context.prioritizeVerticalLayout) {
            row {
              cell()
              buildStandardFormatConfigurationUi(formattedStandardFormatComboBox)
            }.topGap(TopGap.NONE).layout(RowLayout.PARENT_GRID)
          }
          row {
            cell()
            comment("")
              .bindText(formattedStandardFormatPattern)
              .enabledIf(formattedIndividual.not())
          }.topGap(TopGap.NONE).layout(RowLayout.PARENT_GRID)

          row {
            radioButton("Individual format:")
              .bindSelected(formattedIndividual)
              .onChanged { convert(UNIX_TIMESTAMP_MILLIS) }
              .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, UNIX_TIMESTAMP_MILLIS) }
              .gap(RightGap.SMALL)
            expandableTextField()
              .bindText(formattedIndividualFormat)
              .columns(COLUMNS_MEDIUM)
              .whenTextChangedFromUi { convert(UNIX_TIMESTAMP_MILLIS) }
              .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, UNIX_TIMESTAMP_MILLIS) }
              .validationInfo {
                try {
                  if (formattedIndividual.get()) {
                    DateTimeFormatter.ofPattern(it.text)
                  }
                  return@validationInfo null
                } catch (_: Exception) {
                  return@validationInfo ValidationInfo("Invalid individual format", it)
                }
              }
              .enabledIf(formattedIndividual)
          }.layout(RowLayout.PARENT_GRID)
        }

        row {
          comboBox(ALL_AVAILABLE_LOCALES)
            .label("Locale:")
            .bindItem(formattedLocale)
            .columns(COLUMNS_MEDIUM)
            .onChanged { convert(UNIX_TIMESTAMP_MILLIS) }
            .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, UNIX_TIMESTAMP_MILLIS) }
        }.layout(RowLayout.PARENT_GRID)

        row {
          label("")
            .bindText(formattedText)
            .changeFont(scale = 1.1f, style = Font.BOLD)
            .gap(RightGap.SMALL)
          actionButton(CopyAction(FORMATTED_TEXT_DATA_KEY), DatetimeConverter::class.java.name)
        }.topGap(TopGap.SMALL)
      }.topGap(TopGap.NONE)
    }
  }

  override fun afterBuildUi() {
    init()
  }

  override fun reset() {
    init()
  }

  override fun getData(dataId: String): Any? = when {
    TIMESTAMP_SECONDS_CONTENT_DATA_KEY.`is`(dataId) -> stripHtml(currentUnixTimestampSeconds.get(), false)
    TIMESTAMP_MILLIS_CONTENT_DATA_KEY.`is`(dataId) -> stripHtml(currentUnixTimestampMillis.get(), false)
    FORMATTED_TEXT_DATA_KEY.`is`(dataId) -> stripHtml(formattedText.get(), false)
    else -> null
  }

  override fun activated() {
    scheduleCurrentUnixTimestampUpdate(0)
  }

  override fun deactivated() {
    currentUnixTimestampUpdateAlarm.cancelAllRequests()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Row.buildSetToNowButtonUi() {
    button("Set to Now") {
      convertUnixTimeStampMillis.set(System.currentTimeMillis())
      convert(UNIX_TIMESTAMP_MILLIS, 0)
    }
  }

  private fun Row.buildUnixTimeStampMillisTextFieldUi(initialInstant: Instant) {
    textField().validateLongValue(LongRange(0, Long.MAX_VALUE))
      .bindLongTextImproved(convertUnixTimeStampMillis)
      .label("Milliseconds:")
      .text(initialInstant.toEpochMilli().toString())
      .columns(12)
      .whenTextChangedFromUi { convert(UNIX_TIMESTAMP_MILLIS) }
      .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, UNIX_TIMESTAMP_MILLIS) }
  }

  private fun Row.buildUnixTimeStampSecondsTextFieldUi(initialInstant: Instant) {
    textField().validateLongValue(LongRange(0, Long.MAX_VALUE))
      .bindLongTextImproved(convertUnixTimeStampSeconds)
      .label("Seconds:")
      .text(initialInstant.epochSecond.toString())
      .columns(12)
      .whenTextChangedFromUi { convert(UNIX_TIMESTAMP_SECONDS) }
      .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, UNIX_TIMESTAMP_SECONDS) }
  }

  private fun Row.buildStandardFormatConfigurationUi(formattedStandardFormatComboBox: ComboBox<StandardFormat>) {
    checkBox("Add offset")
      .bindSelected(formattedStandardFormatAddOffset)
      .whenStateChangedFromUi { syncFormattedStandardFormatPattern(); convert(UNIX_TIMESTAMP_MILLIS) }
      .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, UNIX_TIMESTAMP_SECONDS) }
      .enabledIf(formattedIndividual.not())
      .visibleIf(ComboBoxPredicate(formattedStandardFormatComboBox) { it?.supportsOffset ?: false })
      .gap(RightGap.SMALL)
    checkBox("Add time zone")
      .bindSelected(formattedStandardFormatAddTimeZone)
      .whenStateChangedFromUi { syncFormattedStandardFormatPattern(); convert(UNIX_TIMESTAMP_MILLIS) }
      .applyToComponent { putUserData(CONVERSION_ORIGIN_KEY, UNIX_TIMESTAMP_SECONDS) }
      .enabledIf(formattedIndividual.not())
      .visibleIf(ComboBoxPredicate(formattedStandardFormatComboBox) { it?.supportsTimeZone ?: false })
  }

  private fun init() {
    syncFormattedStandardFormatPattern()

    convertUnixTimeStampMillis.set(System.currentTimeMillis())
    convert(UNIX_TIMESTAMP_MILLIS, 0)
  }

  private fun syncFormattedStandardFormatPattern() {
    val pattern = formattedStandardFormat.get().buildPattern(
      offset = formattedStandardFormatAddOffset.get(),
      timeZone = formattedStandardFormatAddTimeZone.get()
    )
    formattedStandardFormatPattern.set(pattern)
  }

  private fun createCurrentUnixTimestampUpdate(): Runnable = Runnable {
    currentUnixTimestampSeconds.set("<html><code>${System.currentTimeMillis().div(1000)}</code></html>")
    currentUnixTimestampMillis.set("<html><code>${System.currentTimeMillis()}</code></html>")
    scheduleCurrentUnixTimestampUpdate()
  }

  private fun scheduleCurrentUnixTimestampUpdate(delayMillis: Long = TIMESTAMP_UPDATE_INTERVAL_MILLIS) {
    currentUnixTimestampUpdateAlarm.addRequest(currentUnixTimestampUpdate, delayMillis)
  }

  private fun convert(conversionOrigin: ConversionOrigin, delayMillis: Long = 100) {
    if (validate().any { it.component?.getUserData(CONVERSION_ORIGIN_KEY)?.kind == conversionOrigin.kind }) {
      return
    }

    // Take a snapshot of the input fields since the alarm gets execute with
    // some delay.
    val unixTimeStampSeconds = convertUnixTimeStampSeconds.get()
    val unixTimeStampMillis = convertUnixTimeStampMillis.get()

    val convert: () -> Unit = {
      when (conversionOrigin) {
        UNIX_TIMESTAMP_SECONDS -> {
          val timestampAtSelectedTimeZone = Instant.ofEpochSecond(unixTimeStampSeconds)
            .atZone(selectedTimeZoneId())
          setConvertedValues(timestampAtSelectedTimeZone, conversionOrigin)
        }

        TIME_ZONE, UNIX_TIMESTAMP_MILLIS -> {
          val timestampAtSelectedTimeZone = Instant.ofEpochMilli(unixTimeStampMillis)
            .atZone(selectedTimeZoneId())
          setConvertedValues(timestampAtSelectedTimeZone, conversionOrigin)
        }

        DAY, MONTH, YEAR, HOUR, MINUTE, SECOND -> {
          val year = convertYear.get()
          val month = convertMonth.get()
          val day = convertDay.get()
          val hour = convertHour.get()
          val minute = convertMinute.get()
          val second = convertSecond.get()

          val localDateTime = ZonedDateTime.of(year, month, day, hour, minute, second, 0, selectedTimeZoneId())
          setConvertedValues(localDateTime, conversionOrigin)
        }
      }

      // Trigger validation again to show errors from `ErrorHolder`s
      validate()
    }
    if (!isDisposed && !convertAlarm.isDisposed) {
      convertAlarm.cancelAllRequests()
      convertAlarm.addRequest(convert, delayMillis)
    }
  }

  private fun setConvertedValues(localDateTime: ZonedDateTime, conversionOrigin: ConversionOrigin) {
    val millis = localDateTime.withZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli()
    if (conversionOrigin != UNIX_TIMESTAMP_SECONDS) {
      convertUnixTimeStampSeconds.set(millis.div(1000))
    }
    if (conversionOrigin != UNIX_TIMESTAMP_MILLIS) {
      convertUnixTimeStampMillis.set(millis)
    }
    if (conversionOrigin != YEAR) {
      convertYear.set(localDateTime.year)
    }
    if (conversionOrigin != MONTH) {
      convertMonth.set(localDateTime.monthValue)
    }
    if (conversionOrigin != DAY) {
      convertDay.set(localDateTime.dayOfMonth)
    }
    if (conversionOrigin != HOUR) {
      convertHour.set(localDateTime.hour)
    }
    if (conversionOrigin != MINUTE) {
      convertMinute.set(localDateTime.minute)
    }
    if (conversionOrigin != SECOND) {
      convertSecond.set(localDateTime.second)
    }

    val dayOfYear = localDateTime.get(ChronoField.DAY_OF_YEAR).toLong()
    val weekNumber = localDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toLong()
    val quarterOfYear = localDateTime.get(IsoFields.QUARTER_OF_YEAR).toLong()
    val dayName = localDateTime.dayOfWeek.getDisplayName(FULL_STANDALONE, Locale.getDefault())
    if (context.prioritizeVerticalLayout) {
      dateDetails.set(
        "$dayName; ${formatEnglish(dayOfYear)} day of the year; ${formatEnglish(weekNumber)} week; ${
          formatEnglish(
            quarterOfYear
          )
        } quarter"
      )
    }
    else {
      dateDetails.set(
        "A $dayName, the ${formatEnglish(dayOfYear)} day of the year, in the ${formatEnglish(weekNumber)} week, within the ${
          formatEnglish(
            quarterOfYear
          )
        } quarter."
      )
    }

    formattedText.set("<html><code>${formatDateTime(localDateTime).ifBlank { "No result" }}</code></html>")
  }

  private fun formatDateTime(localDateTime: ZonedDateTime): String =
    try {
      val formatter = if (formattedIndividual.get()) {
        DateTimeFormatter.ofPattern(formattedIndividualFormat.get())
          .withLocale(formattedLocale.get().locale)
          ?.withZone(selectedTimeZoneId())
      }
      else {
        DateTimeFormatter.ofPattern(formattedStandardFormatPattern.get())
          .withLocale(formattedLocale.get().locale)
          .withZone(formattedStandardFormat.get().fixedTimeZone ?: selectedTimeZoneId())
      }
      localDateTime.format(formatter)
    } catch (e: Exception) {
      "Error: ${e.message}"
    }

  private fun selectedTimeZoneId(): ZoneId = ZoneId.of(selectedTimeZoneId.get())

  private fun Row.buildTimestampLabelUi(
    title: String,
    timestampProperty: ObservableProperty<String>,
    contentDataKey: DataKey<String>
  ) {
    panel {
      row {
        label(title).gap(RightGap.SMALL)
        label("")
          .changeFont(scale = 1.5f, style = Font.BOLD)
          .bindText(timestampProperty)
          .gap(RightGap.SMALL)
        actionButton(CopyAction(contentDataKey))
      }.topGap(TopGap.NONE)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<DatetimeConverter> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Date and Time",
      contentTitle = "Date and Time Converter"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> DatetimeConverter) =
      { configuration -> DatetimeConverter(configuration, parentDisposable, context) }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class ConversionOriginKind {

    TIME_ZONE,
    UNIX_TIMESTAMP_SECONDS,
    UNIX_TIMESTAMP_MILLIS,
    LOCAL_DATE_TIME
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class ConversionOrigin(val kind: ConversionOriginKind) {

    TIME_ZONE(ConversionOriginKind.TIME_ZONE),
    UNIX_TIMESTAMP_SECONDS(ConversionOriginKind.UNIX_TIMESTAMP_SECONDS),
    UNIX_TIMESTAMP_MILLIS(ConversionOriginKind.UNIX_TIMESTAMP_MILLIS),
    DAY(ConversionOriginKind.LOCAL_DATE_TIME),
    MONTH(ConversionOriginKind.LOCAL_DATE_TIME),
    YEAR(ConversionOriginKind.LOCAL_DATE_TIME),
    HOUR(ConversionOriginKind.LOCAL_DATE_TIME),
    MINUTE(ConversionOriginKind.LOCAL_DATE_TIME),
    SECOND(ConversionOriginKind.LOCAL_DATE_TIME)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class StandardFormat(
    private val title: String,
    private val pattern: String,
    val supportsOffset: Boolean = true,
    val supportsTimeZone: Boolean = true,
    val fixedTimeZone: ZoneId? = null
  ) {

    ISO_8601("ISO-8601 date time", "yyyy-MM-dd'T'HH:mm:ss.SSS"),
    ISO_8601_UTC(
      title = "ISO-8601 date time at UTC",
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      supportsOffset = false,
      supportsTimeZone = false,
      fixedTimeZone = ZoneOffset.UTC
    ),
    ISO_8601_DATE("ISO-8601 date", "yyyy-MM-dd"),
    ISO_8601_TIME_WITH("ISO-8601 time", "HH:mm:ss"),
    ISO_8601_ORDINAL_DATE("ISO-8601 ordinal date", "yyyy-DDD"),
    ISO_8601_WEEK_DATE("ISO-8601 week date", "YYYY-'W'ww-e"),
    RFC_1123_DATE_TIME("RFC-1123 date time", "EEE, dd MMM yyyy HH:mm:ss");

    fun buildPattern(offset: Boolean, timeZone: Boolean): String {
      val patternBuilder = StringBuilder(pattern)
      if (offset && supportsOffset) {
        patternBuilder.append("XXX")
      }
      if (timeZone && supportsTimeZone) {
        patternBuilder.append("'['VV']'")
      }
      return patternBuilder.toString()
    }

    override fun toString(): String = title
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val TIMESTAMP_SECONDS_CONTENT_DATA_KEY = DataKey.create<String>("timestampSeconds")
    private val TIMESTAMP_MILLIS_CONTENT_DATA_KEY = DataKey.create<String>("timestampMillis")
    private val FORMATTED_TEXT_DATA_KEY = DataKey.create<String>("formattedText")

    private val TIMESTAMP_UPDATE_INTERVAL_MILLIS: Long = Duration.ofSeconds(1).toMillis()

    private const val DEFAULT_FORMATTED_INDIVIDUAL = false
    private val DEFAULT_FORMATTED_LOCALE = LocaleContainer(Locale.getDefault())
    private const val DEFAULT_INDIVIDUAL_FORMAT = ""
    private val DEFAULT_FORMATTED_STANDARD_FORMAT = StandardFormat.ISO_8601_UTC
    private const val DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_OFFSET = true
    private const val DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_TIME_ZONE = false

    private val CONVERSION_ORIGIN_KEY = Key<ConversionOrigin>("conversionOrigin")
  }
}
