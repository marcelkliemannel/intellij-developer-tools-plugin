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
import java.math.BigDecimal.ZERO

class DataSizeConverter(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  MathContextUnitConverter(CONFIGURATION_KEY_PREFIX, configuration, parentDisposable, "Data Size") {
  // -- Properties ---------------------------------------------------------- //

  private val bitDataSizeValue =
    configuration.register(
      "${CONFIGURATION_KEY_PREFIX}bitDataSizeValue",
      ZERO,
      INPUT,
      DEFAULT_BIT_DATA_SIZE_VALUE,
    )
  private val showLargeDataUnits =
    configuration.register(
      "${CONFIGURATION_KEY_PREFIX}showLargeDataUnits",
      DEFAULT_SHOW_LARGE_DATA_UNITS,
    )

  private val dataSizeProperties: List<DataSizeProperty> = createDataProperties()
  private val bitDataSizeProperty = dataSizeProperties.first { it.dataUnit == bitDataUnit }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    NumberSystem.entries.forEach { numberSystem ->
      group(numberSystem.title, true) {
          dataSizeProperties
            .filter { it.dataUnit.numberSystem == numberSystem }
            .forEach { transferRateDataUnitProperty ->
              row {
                  val formattedLabel =
                    label(transferRateDataUnitProperty.inputTitle).gap(RightGap.SMALL)
                  lateinit var formattedFieldTextField: Cell<JBTextField>
                  formattedFieldTextField =
                    textField()
                      .bindText(transferRateDataUnitProperty.formattedValue)
                      .validateBigDecimalValue(ZERO, mathContext) { it.parseBigDecimal() }
                      .resizableColumn()
                      .align(Align.FILL)
                      .whenTextChangedFromUi {
                        convertByInputFieldChange(
                          formattedFieldTextField.component,
                          transferRateDataUnitProperty,
                        )
                      }

                  if (transferRateDataUnitProperty.dataUnit.isLarge) {
                    formattedLabel.visibleIf(showLargeDataUnits)
                    formattedFieldTextField.visibleIf(showLargeDataUnits)
                  }
                }
                .layout(RowLayout.PARENT_GRID)
            }
        }
        .bottomGap(BottomGap.NONE)
        .topGap(TopGap.NONE)
    }
  }

  private fun convertByInputFieldChange(
    inputFieldComponent: JBTextField?,
    inputDataSizeProperty: DataSizeProperty,
  ) {
    if (inputFieldComponent != null && validate().any { it.component == inputFieldComponent }) {
      return
    }

    if (inputDataSizeProperty != bitDataSizeProperty) {
      val inputValue = inputDataSizeProperty.formattedValue.get().parseBigDecimal()
      bitDataSizeValue.set(inputDataSizeProperty.dataUnit.toBits(inputValue, mathContext))
    } else {
      bitDataSizeValue.set(bitDataSizeProperty.formattedValue.get().parseBigDecimal())
    }

    dataSizeProperties
      .filter { it != inputDataSizeProperty }
      .forEach { it.setFromBits(bitDataSizeValue.get(), this) }
  }

  override fun doSync() {
    // During a reset, only the `bitDataSizeValue` will be changed but
    // `convertByInputFieldChange` would overwrite the value with the old
    // formatted value.
    bitDataSizeProperty.formattedValue.set(bitDataSizeValue.get().toFormatted())

    convertByInputFieldChange(null, bitDataSizeProperty)
  }

  @Suppress("UnstableApiUsage")
  override fun Panel.buildAdditionalSettingsUi() {
    row {
      checkBox("Show large data units").bindSelected(showLargeDataUnits).whenStateChangedFromUi {
        sync()
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createDataProperties() =
    dataUnits.map {
      if (it == bitDataUnit) {
        DataSizeProperty(it, bitDataSizeValue).apply {
          formattedValue.set(bitDataSizeValue.get().toFormatted())
        }
      } else {
        DataSizeProperty(it, null)
      }
    }

  // -- Inner Type ---------------------------------------------------------- //

  private class DataSizeProperty(
    val dataUnit: DataUnit,
    val rawValueReference: ValueProperty<BigDecimal>? = null,
  ) {

    val formattedValue: ValueProperty<String> = ValueProperty("0")
    var inputTitle: String = "${dataUnit.name}:"

    fun setFromBits(bits: BigDecimal, unitConverter: MathContextUnitConverter) {
      val result = dataUnit.fromBits(bits, unitConverter.mathContext)
      formattedValue.set(with(unitConverter) { result.toFormatted() })
      rawValueReference?.set(result)
    }

    override fun toString(): String = dataUnit.name
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val CONFIGURATION_KEY_PREFIX = "dataSizeConverter_"
    private val DEFAULT_BIT_DATA_SIZE_VALUE = BigDecimal(1073740000)
    private const val DEFAULT_SHOW_LARGE_DATA_UNITS = false
  }
}
