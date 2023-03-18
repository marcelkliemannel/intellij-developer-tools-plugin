package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.Alarm
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.*
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.UnixTimestampConverter.ConversionOrigin.*
import kotlinx.datetime.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class UnixTimestampConverter(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  DeveloperTool(
    developerToolContext = DeveloperToolContext("Unix Timestamp", "Unix Timestamp Converter"),
    parentDisposable = parentDisposable
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val currentUnixTimestampUpdateAlarm by lazy { Alarm(parentDisposable) }
  private val currentUnixTimestampUpdate: Runnable by lazy { createCurrentUnixTimestampUpdate() }
  private val currentUnixTimestampSeconds: ObservableMutableProperty<String> = AtomicProperty(System.currentTimeMillis().div(1000).toString())
  private val currentUnixTimestampMillis: ObservableMutableProperty<String> = AtomicProperty(System.currentTimeMillis().toString())

  private val convertAlarm by lazy { Alarm(parentDisposable) }
  private var selectedTimeZoneId = configuration.register("selectedTimeZoneId", TimeZone.currentSystemDefault().id)
  private lateinit var unixTimeStampSecondsTextField: JBTextField
  private lateinit var unixTimeStampMillisTextField: JBTextField
  private lateinit var dayTextField: JBTextField
  private lateinit var monthTextField: JBTextField
  private lateinit var yearTextField: JBTextField
  private lateinit var hourTextField: JBTextField
  private lateinit var minuteTextField: JBTextField
  private lateinit var secondTextField: JBTextField
  private lateinit var formattedIso8601: JBLabel
  private lateinit var formattedIso8601Utc: JBLabel
  private lateinit var formattedIso8601WithZone: JBLabel
  private lateinit var formattedIso8601Basic: JBLabel

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    // Validate if selected time zone is still available
    if (!ZoneId.getAvailableZoneIds().contains(selectedTimeZoneId.get())) {
      selectedTimeZoneId.set(TimeZone.currentSystemDefault().id)
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
          unixTimeStampSecondsTextField = textField().validateIntValue(IntRange(0, Int.MAX_VALUE))
                  .text(initialInstant.epochSeconds.toString())
                  .columns(COLUMNS_MEDIUM)
                  .whenTextChangedFromUi { convert(UNIX_TIMESTAMP_SECONDS) }
                  .component
        }
      }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

      group("Unix Timestamp as Milliseconds") {
        row {
          unixTimeStampMillisTextField = textField().validateIntValue(IntRange(0, Int.MAX_VALUE))
                  .text(initialInstant.toEpochMilliseconds().toString())
                  .columns(COLUMNS_MEDIUM)
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
                  .validateIntValue(IntRange(1970, 9999))
                  .whenTextChangedFromUi { convert(YEAR) }
                  .component
          monthTextField = textField().label("Month:")
                  .text(initialLocalDateTime.monthNumber.toString())
                  .validateIntValue(IntRange(1, 12))
                  .whenTextChangedFromUi { convert(MONTH) }
                  .component
          dayTextField = textField().label("Day:")
                  .text(initialLocalDateTime.dayOfMonth.toString())
                  .validateIntValue(IntRange(1, 31))
                  .whenTextChangedFromUi { convert(DAY) }
                  .component
        }.layout(RowLayout.PARENT_GRID)
        row {
          hourTextField = textField().label("Hour:")
                  .text(initialLocalDateTime.hour.toString())
                  .validateIntValue(IntRange(0, 23))
                  .whenTextChangedFromUi { convert(HOUR) }
                  .component
          minuteTextField = textField().label("Minute:")
                  .text(initialLocalDateTime.minute.toString())
                  .validateIntValue(IntRange(0, 59))
                  .whenTextChangedFromUi { convert(MINUTE) }
                  .component
          secondTextField = textField().label("Second:")
                  .text(initialLocalDateTime.second.toString())
                  .validateIntValue(IntRange(0, 59))
                  .whenTextChangedFromUi { convert(SECOND) }
                  .component
        }.layout(RowLayout.PARENT_GRID)
      }.layout(RowLayout.PARENT_GRID).topGap(TopGap.NONE)

      group("Formatted") {
        formattedIso8601 = buildFormattedConvertedDateUi("ISO-8601:", FORMATTED_ISO_8601_CONTENT_DATA_KEY)
        formattedIso8601Utc = buildFormattedConvertedDateUi("ISO-8601 at UTC:", FORMATTED_ISO_8601_UTC_CONTENT_DATA_KEY)
        formattedIso8601WithZone = buildFormattedConvertedDateUi("ISO-8601 time zone:", FORMATTED_ISO_8601_WITH_ZONE_CONTENT_DATA_KEY)
        formattedIso8601Basic = buildFormattedConvertedDateUi("ISO-8601 basic:", FORMATTED_ISO_8601_BASIC_CONTENT_DATA_KEY)
      }.topGap(TopGap.NONE)
    }
  }

  private fun Panel.buildFormattedConvertedDateUi(title: String, contentDataKey: DataKey<String>): JBLabel {
    lateinit var label: JBLabel

    row {
      label(title).gap(RightGap.SMALL)
      label = JBLabel("Calculating...").copyable().apply { font = JBFont.label().toMonospace() }
      val actions = DefaultActionGroup().apply { add(CopyAction(contentDataKey)) }
      cell(label.wrapWithToolBar(title, actions, ToolBarPlace.APPEND))
    }.layout(RowLayout.PARENT_GRID)

    return label
  }

  override fun afterBuildUi() {
    unixTimeStampMillisTextField.text = System.currentTimeMillis().toString()
    convert(UNIX_TIMESTAMP_MILLIS, 0)
  }

  override fun getData(dataId: String): Any? = when {
    TIMESTAMP_SECONDS_CONTENT_DATA_KEY.`is`(dataId) -> currentUnixTimestampSeconds.toString()
    TIMESTAMP_MILLIS_CONTENT_DATA_KEY.`is`(dataId) -> currentUnixTimestampMillis.toString()
    FORMATTED_ISO_8601_CONTENT_DATA_KEY.`is`(dataId) -> formattedIso8601.text
    FORMATTED_ISO_8601_UTC_CONTENT_DATA_KEY.`is`(dataId) -> formattedIso8601Utc.text
    FORMATTED_ISO_8601_WITH_ZONE_CONTENT_DATA_KEY.`is`(dataId) -> formattedIso8601WithZone.text
    FORMATTED_ISO_8601_BASIC_CONTENT_DATA_KEY.`is`(dataId) -> formattedIso8601Basic.text
    else -> null
  }

  override fun activated() {
    scheduleCurrentUnixTimestampUpdate(0)
  }

  override fun deactivated() {
    currentUnixTimestampUpdateAlarm.cancelAllRequests()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

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
    val year = yearTextField.text.toInt()
    val month = monthTextField.text.toInt()
    val day = dayTextField.text.toInt()
    val hour = hourTextField.text.toInt()
    val minute = minuteTextField.text.toInt()
    val second = secondTextField.text.toInt()

    val convert = {
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
          val localDateTime = LocalDateTime.of(year, month, day, hour, minute, second)
          setConvertedValues(localDateTime, conversionOrigin)
        }
      }
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
    if (conversionOrigin != HOUR) {
      hourTextField.text = localDateTime.hour.toString()
    }
    if (conversionOrigin != MINUTE) {
      minuteTextField.text = localDateTime.minute.toString()
    }
    if (conversionOrigin != SECOND) {
      secondTextField.text = localDateTime.second.toString()
    }

    val zoneId = ZoneId.of(selectedTimeZoneId.get())
    formattedIso8601.text = localDateTime.atZone(zoneId).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).toString()
    formattedIso8601Utc.text = localDateTime.atZone(zoneId).format(DateTimeFormatter.ISO_INSTANT).toString()
    formattedIso8601WithZone.text = localDateTime.atZone(zoneId).format(DateTimeFormatter.ISO_DATE_TIME).toString()
    formattedIso8601Basic.text = localDateTime.atZone(zoneId).format(DateTimeFormatter.BASIC_ISO_DATE).toString()
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
        cell(timestampLabel.wrapWithToolBar(UnixTimestampConverter::class.java.name, actions, ToolBarPlace.APPEND))
      }.topGap(TopGap.NONE)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return UnixTimestampConverter(configuration, parentDisposable)
    }
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

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val TIMESTAMP_SECONDS_CONTENT_DATA_KEY = DataKey.create<String>("timestampSeconds")
    private val TIMESTAMP_MILLIS_CONTENT_DATA_KEY = DataKey.create<String>("timestampMillis")

    private val FORMATTED_ISO_8601_CONTENT_DATA_KEY = DataKey.create<String>("formattedIso8601")
    private val FORMATTED_ISO_8601_UTC_CONTENT_DATA_KEY = DataKey.create<String>("formattedIso8601Utc")
    private val FORMATTED_ISO_8601_WITH_ZONE_CONTENT_DATA_KEY = DataKey.create<String>("formattedIso8601WithZone")
    private val FORMATTED_ISO_8601_BASIC_CONTENT_DATA_KEY = DataKey.create<String>("formattedIso8601Basic")

    private val TIMESTAMP_UPDATE_INTERVAL_MILLIS: Long = Duration.ofSeconds(1).toMillis()
    private val TIMESTAMP_TEXT_FONT: JBFont = JBFont.label().toMonospace().biggerOn(3f)
  }
}