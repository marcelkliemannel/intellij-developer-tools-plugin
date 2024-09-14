package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter

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
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.validateBigDecimalValue
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.DataUnit
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.NumberSystem
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.bitDataUnit
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.dataUnits
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.util.concurrent.TimeUnit

internal class TransferRateConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
) : UnitConverter(CONFIGURATION_KEY_PREFIX, configuration, parentDisposable, "Transfer Rate") {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val timeDimension = configuration.register("${CONFIGURATION_KEY_PREFIX}timeDimension", DEFAULT_TIME_DIMENSION)
  private val showLargeDataUnits = configuration.register("${CONFIGURATION_KEY_PREFIX}showLargeDataUnits", DEFAULT_SHOW_LARGE_DATA_UNITS)
  private val useCombinedAbbreviationNotation =
    configuration.register("${CONFIGURATION_KEY_PREFIX}useCombinedAbbreviationNotation", DEFAULT_USE_COMBINED_ABBREVIATION_NOTATION)
  private val bitTransferRateValue = configuration.register("${CONFIGURATION_KEY_PREFIX}bitTransferRateValue", ZERO, INPUT, DEFAULT_BIT_TRANSFER_RATE_VALUE)

  private val transferRateProperties: List<TransferRateProperty> = createTransferRateProperties()
  private val bitTransferRateProperty = transferRateProperties.first { it.dataUnit == bitDataUnit }

  private var lastTimeDimension: TransferRateTimeDimension = timeDimension.get()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    row {
      segmentedButton(TransferRateTimeDimension.entries) { it.title }
        .bind(timeDimension)
        .whenItemSelectedFromUi {
          convertByTimeDimensionChange(Pair(lastTimeDimension, it))
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

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun convertByInputFieldChange(
    inputFieldComponent: JBTextField?,
    inputFieldTransferRateProperty: TransferRateProperty
  ) {
    if (inputFieldComponent != null && validate().any { it.component == inputFieldComponent }) {
      return
    }

    if (inputFieldTransferRateProperty != bitTransferRateProperty) {
      val inputValue = inputFieldTransferRateProperty.formattedValue.get().parseBigDecimal()
      val inputDataUnit = inputFieldTransferRateProperty.dataUnit
      bitTransferRateProperty.convert(inputValue, inputDataUnit, null, this)
    }
    else {
      bitTransferRateValue.set(bitTransferRateProperty.formattedValue.get().parseBigDecimal())
    }

    transferRateProperties
      .filter { it != inputFieldTransferRateProperty && it != bitTransferRateProperty }
      .forEach {
        it.convert(bitTransferRateValue.get(), bitDataUnit, null, this)
      }
  }

  private fun convertByTimeDimensionChange(
    inputOutputTransferRateTimeDimension: Pair<TransferRateTimeDimension, TransferRateTimeDimension>?
  ) {
    if (validate().isNotEmpty()) {
      return
    }

    val inputValue = bitTransferRateProperty.formattedValue.get().parseBigDecimal()
    val inputDataUnit = bitTransferRateProperty.dataUnit
    transferRateProperties
      .forEach {
        it.convert(inputValue, inputDataUnit, inputOutputTransferRateTimeDimension, this)
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
    val timeDimensionTitle = timeDimension.title.lowercase()
    val abbreviationSeparator = if (useCombinedAbbreviationNotation.get()) "p" else "/"
    return "${dataUnit.name}s per $timeDimensionTitle (${dataUnit.abbreviation}$abbreviationSeparator${timeDimension.abbreviation})"
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class TransferRateTimeDimension(
    val title: String,
    val abbreviation: String,
    val seconds: BigDecimal
  ) {

    SECONDS("Seconds", "s", ONE),
    MINUTES("Minutes", "m", TimeUnit.MINUTES.toSeconds(1).toBigDecimal()),
    HOURS("Hours", "h", TimeUnit.HOURS.toSeconds(1).toBigDecimal()),
    DAYS("Days", "d", TimeUnit.DAYS.toSeconds(1).toBigDecimal())
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class TransferRateProperty(
    val dataUnit: DataUnit,
    val rawValue: ValueProperty<BigDecimal>? = null,
    initialFormattedValue: String = "0",
    val createTitle: () -> String
  ) {

    val formattedValue: ValueProperty<String> = ValueProperty(initialFormattedValue)
    val inputTitle: ValueProperty<String> = ValueProperty(createTitle())

    fun sync() {
      inputTitle.set("${createTitle()}:")
    }

    fun convert(
      inputValue: BigDecimal,
      inputDataUnit: DataUnit,
      inputOutputTransferRateTimeDimension: Pair<TransferRateTimeDimension, TransferRateTimeDimension>?,
      unitConverter: UnitConverter
    ) {
      // R₂ = R₁ * (U₁ / U₂) * (T₁ / T₂)
      // Given:
      //  - Input transfer rate: R₁ in units U₁/T₁
      //  - Desired output: R₂ in units U₂/T₂
      // Where:
      //  - U₁ and U₂ are any data units
      //  - T₁ and T₂ are any time units
      //  - (U₁ / U₂) is the conversion factor from U₁ to U₂
      //  - (T₁ / T₂) is the conversion factor from T₁ to T₂
      val mathContext = unitConverter.mathContext
      val dataUnitConversationFactor = inputDataUnit.conversationFactor(mathContext).divide(dataUnit.conversationFactor(mathContext), mathContext)
      var result = inputValue.multiply(dataUnitConversationFactor, mathContext)
      if (inputOutputTransferRateTimeDimension != null) {
        val inputTimeDimension = inputOutputTransferRateTimeDimension.first
        val outputTimeDimension = inputOutputTransferRateTimeDimension.second
        val timeDimensionConversationFactor = inputTimeDimension.seconds.divide(outputTimeDimension.seconds, mathContext)
        result = result.multiply(timeDimensionConversationFactor, mathContext)
      }
      formattedValue.set(with(unitConverter) { result.toFormatted() })
      rawValue?.set(result)
    }

    override fun toString(): String = dataUnit.name
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val CONFIGURATION_KEY_PREFIX = "transferRateConverter_"
    private const val DEFAULT_SHOW_LARGE_DATA_UNITS = false
    private const val DEFAULT_USE_COMBINED_ABBREVIATION_NOTATION = true
    private val DEFAULT_BIT_TRANSFER_RATE_VALUE = BigDecimal.valueOf(1073741824)
    private val DEFAULT_TIME_DIMENSION = TransferRateTimeDimension.SECONDS
  }
}