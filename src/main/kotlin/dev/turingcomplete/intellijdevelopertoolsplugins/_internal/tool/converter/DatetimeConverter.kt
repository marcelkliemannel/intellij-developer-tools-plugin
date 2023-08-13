package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.naturalSorted
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
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
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.CopyAction
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.LocaleContainer
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ToolBarPlace
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.copyable
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.not
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.toMonospace
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.wrapWithToolBar
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.DatetimeConverter.ConversionOrigin.DAY
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.DatetimeConverter.ConversionOrigin.HOUR
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.DatetimeConverter.ConversionOrigin.MINUTE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.DatetimeConverter.ConversionOrigin.MONTH
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.DatetimeConverter.ConversionOrigin.SECOND
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.DatetimeConverter.ConversionOrigin.TIME_ZONE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.DatetimeConverter.ConversionOrigin.UNIX_TIMESTAMP_MILLIS
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.DatetimeConverter.ConversionOrigin.UNIX_TIMESTAMP_SECONDS
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.DatetimeConverter.ConversionOrigin.YEAR
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle.FULL_STANDALONE
import java.util.*

class DatetimeConverter(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  DeveloperTool(parentDisposable) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedTimeZoneId = configuration.register("timeZoneId", TimeZone.currentSystemDefault().id)
  private var formattedStandardFormat = configuration.register("formattedStandardFormat", DEFAULT_FORMATTED_STANDARD_FORMAT)
  private var formattedStandardFormatAddOffset = configuration.register("formattedStandardFormatAddOffset", DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_OFFSET)
  private var formattedStandardFormatAddTimeZone = configuration.register("formattedStandardFormatAddTimeZone", DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_OFFSET)
  private var formattedIndividual = configuration.register("formattedIndividual", DEFAULT_FORMATTED_INDIVIDUAL)
  private var formattedLocale = configuration.register("formattedLocale", DEFAULT_FORMATTED_LOCALE)
  private var formattedIndividualFormat = configuration.register("formattedIndividualFormat", DEFAULT_INDIVIDUAL_FORMAT)

  private var formattedStandardFormatPattern = ValueProperty("")
  private var formattedText = ValueProperty("No result")
  private var dayOfWeek = ValueProperty("")

  private val currentUnixTimestampUpdateAlarm by lazy { Alarm(parentDisposable) }
  private val currentUnixTimestampUpdate: Runnable by lazy { createCurrentUnixTimestampUpdate() }
  private val currentUnixTimestampSeconds = ValueProperty(System.currentTimeMillis().div(1000).toString())

  private val currentUnixTimestampMillis = ValueProperty(System.currentTimeMillis().toString())
  private val convertAlarm by lazy { Alarm(parentDisposable) }
  private lateinit var unixTimeStampSecondsTextField: JBTextField
  private lateinit var unixTimeStampMillisTextField: JBTextField
  private lateinit var dayTextField: JBTextField
  private lateinit var monthTextField: JBTextField
  private lateinit var yearTextField: JBTextField
  private lateinit var hourTextField: JBTextField
  private lateinit var minuteTextField: JBTextField
  private lateinit var secondTextField: JBTextField

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    // Validate if selected time zone and formatted local  is still available
    if (!ZoneId.getAvailableZoneIds().contains(selectedTimeZoneId.get())) {
      selectedTimeZoneId.set(TimeZone.currentSystemDefault().id)
    }
    if (!LOCALES.contains(formattedLocale.get())) {
      formattedLocale.set(DEFAULT_FORMATTED_LOCALE)
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    group("Current Unix Timestamp") {
      row {
        buildTimestampLabelUi("Seconds:", currentUnixTimestampSeconds, TIMESTAMP_SECONDS_CONTENT_DATA_KEY)
        buildTimestampLabelUi("Milliseconds:", currentUnixTimestampMillis, TIMESTAMP_MILLIS_CONTENT_DATA_KEY)
      }
    }

    group("Convert") {
      val initialInstant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
      val initialLocalDateTime = initialInstant.toLocalDateTime(TimeZone.of(selectedTimeZoneId.get()))

      group("Unix Timestamp as Seconds") {
        row {
          unixTimeStampSecondsTextField = textField().validateLongValue(LongRange(0, Long.MAX_VALUE))
            .label("Seconds:")
            .text(initialInstant.epochSeconds.toString())
            .columns(COLUMNS_SHORT)
            .whenTextChangedFromUi { convert(UNIX_TIMESTAMP_SECONDS) }
            .component

          unixTimeStampMillisTextField = textField().validateLongValue(LongRange(0, Long.MAX_VALUE))
            .label("Milliseconds:")
            .text(initialInstant.toEpochMilliseconds().toString())
            .columns(COLUMNS_SHORT)
            .whenTextChangedFromUi { convert(UNIX_TIMESTAMP_MILLIS) }
            .component
        }
      }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      group("Date and Time") {
        row {
          comboBox(ZoneId.getAvailableZoneIds().sorted())
            .label("Time zone:")
            .bindItem(selectedTimeZoneId)
            .whenItemSelectedFromUi {
              convert(TIME_ZONE)
            }
        }.layout(RowLayout.PARENT_GRID)
        row {
          yearTextField = textField().label("Year:")
            .text(initialLocalDateTime.year.toString())
            .columns(COLUMNS_TINY)
            .validateLongValue(LongRange(1970, 9999))
            .whenTextChangedFromUi { convert(YEAR) }
            .component
          monthTextField = textField().label("Month:")
            .text(initialLocalDateTime.monthNumber.toString())
            .columns(COLUMNS_TINY)
            .validateLongValue(LongRange(1, 12))
            .whenTextChangedFromUi { convert(MONTH) }
            .component
          dayTextField = textField().label("Day:")
            .text(initialLocalDateTime.dayOfMonth.toString())
            .columns(COLUMNS_TINY)
            .validateLongValue(LongRange(1, 31))
            .whenTextChangedFromUi { convert(DAY) }
            .gap(RightGap.SMALL)
            .component
          comment("")
            .bindText(dayOfWeek)
        }.layout(RowLayout.PARENT_GRID)
        row {
          hourTextField = textField().label("Hour:")
            .text(initialLocalDateTime.hour.toString())
            .columns(COLUMNS_TINY)
            .validateLongValue(LongRange(0, 23))
            .whenTextChangedFromUi { convert(HOUR) }
            .component
          minuteTextField = textField().label("Minute:")
            .text(initialLocalDateTime.minute.toString())
            .columns(COLUMNS_TINY)
            .validateLongValue(LongRange(0, 59))
            .whenTextChangedFromUi { convert(MINUTE) }
            .component
          secondTextField = textField().label("Second:")
            .text(initialLocalDateTime.second.toString())
            .columns(COLUMNS_TINY)
            .validateLongValue(LongRange(0, 59))
            .whenTextChangedFromUi { convert(SECOND) }
            .component
        }.layout(RowLayout.PARENT_GRID)
      }.layout(RowLayout.PARENT_GRID).topGap(TopGap.NONE)

      group("Formatted") {
        buttonsGroup {
          row {
            radioButton("Standard format:")
              .bindSelected(formattedIndividual.not())
              .onChanged { convert(UNIX_TIMESTAMP_MILLIS) }
              .gap(RightGap.SMALL)
            val formattedStandardFormatComboBox = comboBox(StandardFormat.values().toList())
              .bindItem(formattedStandardFormat)
              .whenItemSelectedFromUi { syncFormattedStandardFormatPattern(); convert(UNIX_TIMESTAMP_MILLIS) }
              .enabledIf(formattedIndividual.not())
              .gap(RightGap.SMALL)
              .component
            checkBox("Add offset")
              .bindSelected(formattedStandardFormatAddOffset)
              .whenStateChangedFromUi { syncFormattedStandardFormatPattern(); convert(UNIX_TIMESTAMP_MILLIS) }
              .enabledIf(formattedIndividual.not())
              .visibleIf(ComboBoxPredicate(formattedStandardFormatComboBox) { it?.supportsOffset ?: false })
              .gap(RightGap.SMALL)
            checkBox("Add time zone")
              .bindSelected(formattedStandardFormatAddTimeZone)
              .whenStateChangedFromUi { syncFormattedStandardFormatPattern(); convert(UNIX_TIMESTAMP_MILLIS) }
              .enabledIf(formattedIndividual.not())
              .visibleIf(ComboBoxPredicate(formattedStandardFormatComboBox) { it?.supportsTimeZone ?: false })
          }.layout(RowLayout.PARENT_GRID).bottomGap(BottomGap.NONE)
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
              .gap(RightGap.SMALL)
            textField()
              .bindText(formattedIndividualFormat)
              .whenTextChangedFromUi { convert(UNIX_TIMESTAMP_MILLIS) }
              .validationInfo {
                try {
                  if (formattedIndividual.get()) {
                    DateTimeFormatter.ofPattern(it.text)
                  }
                  return@validationInfo null
                } catch (e: Exception) {
                  return@validationInfo ValidationInfo("Invalid individual format", it)
                }
              }
              .enabledIf(formattedIndividual)
          }.layout(RowLayout.PARENT_GRID)
        }

        row {
          comboBox(LOCALES)
            .label("Locale:")
            .bindItem(formattedLocale)
            .onChanged { convert(UNIX_TIMESTAMP_MILLIS) }
        }.layout(RowLayout.PARENT_GRID)

        row {
          label("")
            .bindText(formattedText)
            .applyToComponent { font = FORMATTED_TEXT_FONT }
            .gap(RightGap.SMALL)
          actionButton(CopyAction(FORMATTED_TEXT_DATA_KEY), DatetimeConverter::class.java.name)
        }.topGap(TopGap.SMALL)
      }.topGap(TopGap.NONE)
    }
  }

  override fun afterBuildUi() {
    reset()
  }

  override fun reset() {
    formattedIndividual.set(DEFAULT_FORMATTED_INDIVIDUAL)
    formattedIndividualFormat.set(DEFAULT_INDIVIDUAL_FORMAT)
    formattedStandardFormat.set(DEFAULT_FORMATTED_STANDARD_FORMAT)
    formattedStandardFormatAddOffset.set(DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_OFFSET)
    formattedStandardFormatAddTimeZone.set(DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_TIME_ZONE)
    syncFormattedStandardFormatPattern()

    unixTimeStampMillisTextField.text = System.currentTimeMillis().toString()
    convert(UNIX_TIMESTAMP_MILLIS, 0)
  }

  override fun getData(dataId: String): Any? = when {
    TIMESTAMP_SECONDS_CONTENT_DATA_KEY.`is`(dataId) -> currentUnixTimestampSeconds.get()
    TIMESTAMP_MILLIS_CONTENT_DATA_KEY.`is`(dataId) -> currentUnixTimestampMillis.get()
    FORMATTED_TEXT_DATA_KEY.`is`(dataId) -> formattedText.get()
    else -> null
  }

  override fun activated() {
    scheduleCurrentUnixTimestampUpdate(0)
  }

  override fun deactivated() {
    currentUnixTimestampUpdateAlarm.cancelAllRequests()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun syncFormattedStandardFormatPattern() {
    val pattern = formattedStandardFormat.get().buildPattern(
      offset = formattedStandardFormatAddOffset.get(),
      timeZone = formattedStandardFormatAddTimeZone.get()
    )
    formattedStandardFormatPattern.set(pattern)
  }

  private fun createCurrentUnixTimestampUpdate(): Runnable = Runnable {
    currentUnixTimestampSeconds.set(System.currentTimeMillis().div(1000).toString())
    currentUnixTimestampMillis.set(System.currentTimeMillis().toString())
    scheduleCurrentUnixTimestampUpdate()
  }

  private fun scheduleCurrentUnixTimestampUpdate(delayMillis: Long = TIMESTAMP_UPDATE_INTERVAL_MILLIS) {
    currentUnixTimestampUpdateAlarm.addRequest(currentUnixTimestampUpdate, delayMillis)
  }

  private fun convert(conversionOrigin: ConversionOrigin, delayMillis: Long = 100) {
    if (validate().isNotEmpty()) {
      return
    }

    // Take a snapshot of the input fields since the alarm gets execute with
    // some delay.
    val unixTimeStampSeconds = unixTimeStampSecondsTextField.text.toLong()
    val unixTimeStampMillis = unixTimeStampMillisTextField.text.toLong()

    val convert: () -> Unit = {
      when (conversionOrigin) {
        UNIX_TIMESTAMP_SECONDS -> {
          val localDateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(unixTimeStampSeconds), ZoneId.of(selectedTimeZoneId.get()))
          setConvertedValues(localDateTime, conversionOrigin)
        }

        TIME_ZONE, UNIX_TIMESTAMP_MILLIS -> {
          val localDateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(unixTimeStampMillis), ZoneId.of(selectedTimeZoneId.get()))
          setConvertedValues(localDateTime, conversionOrigin)
        }

        DAY, MONTH, YEAR, HOUR, MINUTE, SECOND -> {
          val year = yearTextField.text.toInt()
          val month = monthTextField.text.toInt()
          val day = dayTextField.text.toInt()
          val hour = hourTextField.text.toInt()
          val minute = minuteTextField.text.toInt()
          val second = secondTextField.text.toInt()

          val localDateTime = LocalDateTime.of(year, month, day, hour, minute, second)
          setConvertedValues(localDateTime, conversionOrigin)
        }
      }

      // Trigger validation again to show errors from `ErrorHolder`s
      validate()
    }
    convertAlarm.cancelAllRequests()
    convertAlarm.addRequest(convert, delayMillis)
  }

  private fun setConvertedValues(localDateTime: LocalDateTime, conversionOrigin: ConversionOrigin) {
    val millis = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
    if (conversionOrigin != UNIX_TIMESTAMP_SECONDS) {
      unixTimeStampSecondsTextField.text = millis.div(1000).toString()
    }
    if (conversionOrigin != UNIX_TIMESTAMP_MILLIS) {
      unixTimeStampMillisTextField.text = millis.toString()
    }
    if (conversionOrigin != YEAR) {
      yearTextField.text = localDateTime.year.toString()
    }
    if (conversionOrigin != MONTH) {
      monthTextField.text = localDateTime.monthValue.toString()
    }
    if (conversionOrigin != DAY) {
      dayTextField.text = localDateTime.dayOfMonth.toString()
    }
    dayOfWeek.set(localDateTime.dayOfWeek.getDisplayName(FULL_STANDALONE, Locale.getDefault()))
    if (conversionOrigin != HOUR) {
      hourTextField.text = localDateTime.hour.toString()
    }
    if (conversionOrigin != MINUTE) {
      minuteTextField.text = localDateTime.minute.toString()
    }
    if (conversionOrigin != SECOND) {
      secondTextField.text = localDateTime.second.toString()
    }
    formattedText.set(formatDateTime(localDateTime).ifBlank { "No result" })
  }

  private fun formatDateTime(localDateTime: LocalDateTime): String =
    try {
      val formatter = if (formattedIndividual.get()) {
        DateTimeFormatter.ofPattern(formattedIndividualFormat.get())
          .withLocale(formattedLocale.get().locale)
          ?.withZone(ZoneId.of(selectedTimeZoneId.get()))
      }
      else {
        DateTimeFormatter.ofPattern(formattedStandardFormatPattern.get())
          .withLocale(formattedLocale.get().locale)
          .withZone(formattedStandardFormat.get().fixedTimeZone ?: ZoneId.of(selectedTimeZoneId.get()))
      }
      localDateTime.atZone(ZoneId.of(selectedTimeZoneId.get())).format(formatter)
    } catch (e: Exception) {
      "Error: ${e.message}"
    }

  private fun Row.buildTimestampLabelUi(
    title: String,
    timestampProperty: ObservableProperty<String>,
    contentDataKey: DataKey<String>
  ) {
    val timestampLabel = JBLabel().apply { font = TIMESTAMP_TEXT_FONT }.copyable().bind(timestampProperty)
    val actions = DefaultActionGroup().apply {
      add(CopyAction(contentDataKey))
    }
    panel {
      row {
        label(title).gap(RightGap.SMALL)
        cell(timestampLabel.wrapWithToolBar(DatetimeConverter::class.java.name, actions, ToolBarPlace.APPEND))
      }.topGap(TopGap.NONE)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<DatetimeConverter> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "Date Time",
      contentTitle = "Date Time Converter"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> DatetimeConverter) =
      { configuration -> DatetimeConverter(configuration, parentDisposable) }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class ConversionOrigin {

    TIME_ZONE,
    UNIX_TIMESTAMP_SECONDS,
    UNIX_TIMESTAMP_MILLIS,
    DAY,
    MONTH,
    YEAR,
    HOUR,
    MINUTE,
    SECOND
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
      title ="ISO-8601 date time at UTC",
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
    private val TIMESTAMP_TEXT_FONT: JBFont = JBFont.label().toMonospace().biggerOn(3f)

    private const val DEFAULT_FORMATTED_INDIVIDUAL = false
    private val DEFAULT_FORMATTED_LOCALE = LocaleContainer(Locale.getDefault())
    private const val DEFAULT_INDIVIDUAL_FORMAT = ""
    private val DEFAULT_FORMATTED_STANDARD_FORMAT = StandardFormat.ISO_8601
    private const val DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_OFFSET = true
    private const val DEFAULT_FORMATTED_STANDARD_FORMAT_ADD_TIME_ZONE = false

    private val FORMATTED_TEXT_FONT = JBFont.label().toMonospace().biggerOn(1.5f)

    private val LOCALES = Locale.getAvailableLocales()
      .filter { it.displayName.isNotBlank() }
      .map { LocaleContainer(it) }
      .naturalSorted()
  }
}