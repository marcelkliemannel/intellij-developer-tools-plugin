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
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.validateBigDecimalValue
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.DataUnit
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.NumberSystem
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.bitDataUnit
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.dataUnits
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

class DataSizeConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : UnitConverter(CONFIGURATION_KEY_PREFIX, configuration, parentDisposable, "Data Size") {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val bitDataSizeValue = configuration.register("${CONFIGURATION_KEY_PREFIX}bitDataSizeValue", DEFAULT_BIT_DATA_SIZE_VALUE)
  private val showLargeDataUnits = configuration.register("${CONFIGURATION_KEY_PREFIX}showLargeDataUnits", DEFAULT_SHOW_LARGE_DATA_UNITS)

  private val dataSizeProperties: List<DataSizeProperty> = createDataProperties()
  private val bitDataSizeProperty = dataSizeProperties.first { it.dataUnit == bitDataUnit }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    NumberSystem.entries.forEach { numberSystem ->
      group(numberSystem.title, true) {
        dataSizeProperties
          .filter { it.dataUnit.numberSystem == numberSystem }
          .forEach { transferRateDataUnitProperty ->
            row {
              val formattedLabel = label(transferRateDataUnitProperty.inputTitle)
                .gap(RightGap.SMALL)
              lateinit var formattedFieldTextField: Cell<JBTextField>
              formattedFieldTextField = textField()
                .bindText(transferRateDataUnitProperty.formattedValue)
                .validateBigDecimalValue(ZERO, mathContext) { it.parseBigDecimal() }
                .resizableColumn()
                .align(Align.FILL)
                .whenTextChangedFromUi {
                  convert(formattedFieldTextField.component, transferRateDataUnitProperty)
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

  private fun convert(
    inputFieldComponent: JBTextField?,
    dataSizeProperty: DataSizeProperty
  ) {
    if (inputFieldComponent != null && validate().any { it.component == inputFieldComponent }) {
      return
    }

    if (dataSizeProperty != bitDataSizeProperty) {
      val inputValue = dataSizeProperty.formattedValue.get().parseBigDecimal()
      val inputDataUnit = dataSizeProperty.dataUnit
      bitDataSizeProperty.convert(inputValue, inputDataUnit, this)
    }
    else {
      bitDataSizeValue.set(bitDataSizeProperty.formattedValue.get().parseBigDecimal())
    }

    dataSizeProperties
      .filter { it != dataSizeProperty && it != bitDataSizeProperty }
      .forEach { it.convert(bitDataSizeValue.get(), bitDataUnit, this) }
  }

  override fun doSync() {
    convert(null, bitDataSizeProperty)
  }

  override fun Panel.buildAdditionalSettingsUi() {
    row {
      checkBox("Show large data units")
        .bindSelected(showLargeDataUnits)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createDataProperties() = dataUnits.map {
    if (it == bitDataUnit) {
      DataSizeProperty(it, bitDataSizeValue).apply { formattedValue.set(bitDataSizeValue.get().toFormatted()) }
    }
    else {
      DataSizeProperty(it, null)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class DataSizeProperty(
    val dataUnit: DataUnit,
    val rawValue: ValueProperty<BigDecimal>? = null
  ) {

    val formattedValue: ValueProperty<String> = ValueProperty("0")
    var inputTitle: String = "${dataUnit.name}:"

    fun convert(
      inputValue: BigDecimal,
      inputDataUnit: DataUnit,
      unitConverter: UnitConverter
    ) {
      val mathContext = unitConverter.mathContext
      val result = inputValue
        .multiply(inputDataUnit.conversationFactor(mathContext), mathContext)
        .divide(dataUnit.conversationFactor(mathContext), mathContext)
      formattedValue.set(with(unitConverter) { result.toFormatted() })
      rawValue?.set(result)
    }

    override fun toString(): String = dataUnit.name
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val CONFIGURATION_KEY_PREFIX = "dataSizeConverter_"
    private val DEFAULT_BIT_DATA_SIZE_VALUE = BigDecimal(1234567890)
    private const val DEFAULT_SHOW_LARGE_DATA_UNITS = false
  }
}