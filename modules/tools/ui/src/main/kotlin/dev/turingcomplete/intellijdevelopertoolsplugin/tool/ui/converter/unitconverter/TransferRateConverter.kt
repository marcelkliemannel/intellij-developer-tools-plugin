package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.validateBigDecimalValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.DataUnits.DataUnit
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.DataUnits.NumberSystem
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.DataUnits.bitDataUnit
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.DataUnits.dataUnits
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.util.concurrent.TimeUnit

class TransferRateConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
) : MathContextUnitConverter(CONFIGURATION_KEY_PREFIX, configuration, parentDisposable, "Transfer Rate") {
  // -- Properties ---------------------------------------------------------- //

  private val timeDimension = configuration.register("${CONFIGURATION_KEY_PREFIX}timeDimension", DEFAULT_TIME_DIMENSION)
  private val showLargeDataUnits = configuration.register("${CONFIGURATION_KEY_PREFIX}showLargeDataUnits", DEFAULT_SHOW_LARGE_DATA_UNITS)
  private val useCombinedAbbreviationNotation =
    configuration.register("${CONFIGURATION_KEY_PREFIX}useCombinedAbbreviationNotation", DEFAULT_USE_COMBINED_ABBREVIATION_NOTATION)
  private val bitTransferRateValue = configuration.register("${CONFIGURATION_KEY_PREFIX}bitTransferRateValue", ZERO, INPUT, DEFAULT_BIT_TRANSFER_RATE_VALUE)

  private val transferRateProperties: List<TransferRateProperty> = createTransferRateProperties()
  private val bitTransferRateProperty = transferRateProperties.first { it.dataUnit == bitDataUnit }

  private var lastTimeDimension: TransferRateTimeDimension = timeDimension.get()

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    row {
      segmentedButton(TransferRateTimeDimension.entries) { text = it.title }
        .bind(timeDimension)
        .whenItemSelectedFromUi {
          convertByTimeDimensionChange(lastTimeDimension, it)
          lastTimeDimension = it
        }
    }

    NumberSystem.entries.forEach { numberSystem ->
      group(numberSystem.title, true) {
        transferRateProperties
          .filter { it.dataUnit.numberSystem == numberSystem }
          .forEach { transferRateDataUnitProperty ->
            row {
              val formattedLabel = label("")
                .bindText(transferRateDataUnitProperty.inputTitle)
                .gap(RightGap.SMALL)
              lateinit var formattedFieldTextField: Cell<JBTextField>
              formattedFieldTextField = textField()
                .bindText(transferRateDataUnitProperty.formattedValue)
                .validateBigDecimalValue(ZERO, mathContext) { it.parseBigDecimal() }
                .resizableColumn()
                .align(Align.FILL)
                .whenTextChangedFromUi {
                  convertByInputFieldChange(formattedFieldTextField.component, transferRateDataUnitProperty)
                }

              if (transferRateDataUnitProperty.dataUnit.isLarge) {
                formattedLabel.visibleIf(showLargeDataUnits)
                formattedFieldTextField.visibleIf(showLargeDataUnits)
              }
            }.layout(RowLayout.PARENT_GRID)
          }
      }.bottomGap(BottomGap.NONE).topGap(TopGap.NONE)
    }
  }

  @Suppress("UnstableApiUsage")
  override fun Panel.buildAdditionalSettingsUi() {
    row {
      checkBox("Show large data units")
        .bindSelected(showLargeDataUnits)
        .whenStateChangedFromUi { sync() }
    }
    row {
      checkBox("Use combined abbreviation notation")
        .bindSelected(useCombinedAbbreviationNotation)
        .whenStateChangedFromUi { sync() }
    }
  }

  override fun doSync() {
    // During a reset, only the `bitTransferRateValue` will be changed but
    // `convertByInputFieldChange` would overwrite the value with the old
    // formatted value.
    bitTransferRateProperty.formattedValue.set(bitTransferRateValue.get().toFormatted())
    convertByInputFieldChange(null, bitTransferRateProperty)

    transferRateProperties.forEach {
      it.sync()
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun convertByInputFieldChange(
    inputFieldComponent: JBTextField?,
    inputFieldTransferRateProperty: TransferRateProperty
  ) {
    if (inputFieldComponent != null && validate().any { it.component == inputFieldComponent }) {
      return
    }

    if (inputFieldTransferRateProperty != bitTransferRateProperty) {
      val inputValue = inputFieldTransferRateProperty.formattedValue.get().parseBigDecimal()
      bitTransferRateValue.set(inputFieldTransferRateProperty.dataUnit.toBits(inputValue, mathContext))
    }
    else {
      bitTransferRateValue.set(bitTransferRateProperty.formattedValue.get().parseBigDecimal())
    }

    transferRateProperties
      .filter { it != inputFieldTransferRateProperty }
      .forEach { it.setFromBits(bitTransferRateValue.get(), this) }
  }

  private fun convertByTimeDimensionChange(
    originTimeDimension: TransferRateTimeDimension,
    targetTimeDimension: TransferRateTimeDimension
  ) {
    // No validation, always use the last value bits value

    val bits = bitTransferRateValue.get()
    transferRateProperties.forEach {
      it.setFromTimeDimensionChange(bits, originTimeDimension, targetTimeDimension, this)
      it.sync()
    }
  }

  private fun createTransferRateProperties() = dataUnits.map {
    if (it == bitDataUnit) {
      TransferRateProperty(it, bitTransferRateValue, bitTransferRateValue.get().toFormatted()) { createTitle(it, timeDimension) }
        .apply { formattedValue.set(bitTransferRateValue.get().toFormatted()) }
    }
    else {
      TransferRateProperty(it, null) { createTitle(it, timeDimension) }
    }
  }

  private fun createTitle(
    dataUnit: DataUnit,
    timeDimensionProperty: ValueProperty<TransferRateTimeDimension>
  ): String {
    val timeDimension = timeDimensionProperty.get()
    val abbreviationSeparator = if (useCombinedAbbreviationNotation.get()) "p" else "/"
    return "${dataUnit.name}s per ${timeDimension.shortTitle} (${dataUnit.abbreviation}$abbreviationSeparator${timeDimension.abbreviation})"
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class TransferRateTimeDimension(
    val title: String,
    val shortTitle: String,
    val abbreviation: String,
    val seconds: BigDecimal
  ) {

    SECONDS("Seconds", "sec.", "s", ONE),
    MINUTES("Minutes", "min.", "m", TimeUnit.MINUTES.toSeconds(1).toBigDecimal()),
    HOURS("Hours", "hours", "h", TimeUnit.HOURS.toSeconds(1).toBigDecimal()),
    DAYS("Days", "days", "d", TimeUnit.DAYS.toSeconds(1).toBigDecimal())
  }

  // -- Inner Type ---------------------------------------------------------- //

  class TransferRateProperty(
    val dataUnit: DataUnit,
    private val rawValueReference: ValueProperty<BigDecimal>? = null,
    initialFormattedValue: String = "0",
    val createTitle: () -> String
  ) {

    val formattedValue: ValueProperty<String> = ValueProperty(initialFormattedValue)
    val inputTitle: ValueProperty<String> = ValueProperty(createTitle())

    fun sync() {
      inputTitle.set("${createTitle()}:")
    }

    fun setFromBits(
      bits: BigDecimal,
      unitConverter: MathContextUnitConverter
    ) {
      val result = dataUnit.fromBits(bits, unitConverter.mathContext)
      formattedValue.set(with(unitConverter) { result.toFormatted() })
      rawValueReference?.set(result)
    }

    fun setFromTimeDimensionChange(
      bits: BigDecimal,
      originTimeDimension: TransferRateTimeDimension,
      targetTimeDimension: TransferRateTimeDimension,
      unitConverter: MathContextUnitConverter
    ) {
      val mathContext = unitConverter.mathContext
      val timeFactor = originTimeDimension.seconds.divide(targetTimeDimension.seconds, mathContext)
      setFromBits(bits.multiply(timeFactor, mathContext), unitConverter)
    }

    override fun toString(): String = dataUnit.name
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val CONFIGURATION_KEY_PREFIX = "transferRateConverter_"
    private const val DEFAULT_SHOW_LARGE_DATA_UNITS = false
    private const val DEFAULT_USE_COMBINED_ABBREVIATION_NOTATION = true
    private val DEFAULT_BIT_TRANSFER_RATE_VALUE = BigDecimal.valueOf(1073741824)
    private val DEFAULT_TIME_DIMENSION = TransferRateTimeDimension.SECONDS
  }
}
