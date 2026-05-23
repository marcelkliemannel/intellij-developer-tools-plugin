package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter

import com.intellij.testFramework.junit5.TestApplication
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.DataUnits.BaseDataUnit.BIT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.DataUnits.BaseDataUnit.BYTE
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.DataUnits.NumberSystem.BINARY
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.DataUnits.NumberSystem.DECIMAL
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.TransferRateConverter.TransferRateTimeDimension.HOURS
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.TransferRateConverter.TransferRateTimeDimension.MINUTES
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter.TransferRateConverter.TransferRateTimeDimension.SECONDS
import java.math.BigDecimal
import java.math.MathContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@TestApplication
class UnitConverterConversionTest {
  // -- Properties ---------------------------------------------------------- //

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @Test
  fun `test data unit conversions to bits`() {
    val kilobit = dataUnit("Kilobit", BIT, DECIMAL)
    val kibibit = dataUnit("Kibibit", BIT, BINARY)
    val kilobyte = dataUnit("Kilobyte", BYTE, DECIMAL)
    val kibibyte = dataUnit("Kibibyte", BYTE, BINARY)

    assertThat(kilobit.toBits(BigDecimal.ONE, MATH_CONTEXT)).isEqualByComparingTo("1000")
    assertThat(kibibit.toBits(BigDecimal.ONE, MATH_CONTEXT)).isEqualByComparingTo("1024")
    assertThat(kilobyte.toBits(BigDecimal.ONE, MATH_CONTEXT)).isEqualByComparingTo("8000")
    assertThat(kibibyte.toBits(BigDecimal.ONE, MATH_CONTEXT)).isEqualByComparingTo("8192")
  }

  @Test
  fun `test data unit conversions from bits`() {
    val kilobit = dataUnit("Kilobit", BIT, DECIMAL)
    val kibibit = dataUnit("Kibibit", BIT, BINARY)
    val kilobyte = dataUnit("Kilobyte", BYTE, DECIMAL)
    val kibibyte = dataUnit("Kibibyte", BYTE, BINARY)

    assertThat(kilobit.fromBits(BigDecimal("1000"), MATH_CONTEXT)).isEqualByComparingTo("1")
    assertThat(kibibit.fromBits(BigDecimal("1024"), MATH_CONTEXT)).isEqualByComparingTo("1")
    assertThat(kilobyte.fromBits(BigDecimal("8000"), MATH_CONTEXT)).isEqualByComparingTo("1")
    assertThat(kibibyte.fromBits(BigDecimal("8192"), MATH_CONTEXT)).isEqualByComparingTo("1")
  }

  @Test
  fun `test binary bit abbreviations use binary prefix casing`() {
    assertThat(dataUnit("Kibibit", BIT, BINARY).abbreviation).isEqualTo("Kib")
    assertThat(dataUnit("Mebibit", BIT, BINARY).abbreviation).isEqualTo("Mib")
  }

  @Test
  fun `test transfer rate time dimension conversion keeps rate constant`() {
    assertThat(SECONDS.convertBitsTo(BigDecimal("60"), MINUTES, MATH_CONTEXT))
      .isEqualByComparingTo("3600")
    assertThat(MINUTES.convertBitsTo(BigDecimal("3600"), SECONDS, MATH_CONTEXT))
      .isEqualByComparingTo("60")
    assertThat(HOURS.convertBitsTo(BigDecimal("7200"), MINUTES, MATH_CONTEXT))
      .isEqualByComparingTo("120")
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun dataUnit(
    name: String,
    baseDataUnit: DataUnits.BaseDataUnit,
    numberSystem: DataUnits.NumberSystem,
  ): DataUnits.DataUnit =
    DataUnits.dataUnits.first {
      it.name == name && it.baseDataUnit == baseDataUnit && it.numberSystem == numberSystem
    }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val MATH_CONTEXT = MathContext(50)
  }
}
