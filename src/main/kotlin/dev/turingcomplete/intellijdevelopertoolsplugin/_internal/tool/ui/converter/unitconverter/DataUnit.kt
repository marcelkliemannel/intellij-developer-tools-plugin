package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter

import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.BaseDataUnit.BIT
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.BaseDataUnit.BYTE
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.NumberSystem.BASIC
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.NumberSystem.BINARY
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.NumberSystem.DECIMAL
import java.math.BigDecimal
import java.math.MathContext

internal object DataUnits {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val bigDecimalEight = BigDecimal.valueOf(8)

  val bitDataUnit = DataUnit(
    name = "Bit",
    abbreviation = "b",
    isLarge = false,
    baseDataUnit = BIT,
    numberSystem = BASIC,
    exponent = 0
  )

  val dataUnits: List<DataUnit> = listOf(
    listOf(
      bitDataUnit,
      DataUnit(
        name = "Byte",
        abbreviation = "B",
        isLarge = false,
        baseDataUnit = BIT,
        numberSystem = BASIC,
        exponent = 3
      )
    ),
    createDataUnits(
      decimalPrefix = "Kilo",
      binaryPrefix = "Kibi",
      decimalAbbreviationFirstLetter = 'k',
      binaryAbbreviationFirstLetter = 'K',
      decimalExponent = 3,
      binaryExponent = 10,
      isLarge = false
    ),
    createDataUnits(
      decimalPrefix = "Mega",
      binaryPrefix = "Mebi",
      decimalAbbreviationFirstLetter = 'M',
      binaryAbbreviationFirstLetter = 'M',
      decimalExponent = 6,
      binaryExponent = 20,
      isLarge = false
    ),
    createDataUnits(
      decimalPrefix = "Giga",
      binaryPrefix = "Gibi",
      decimalAbbreviationFirstLetter = 'G',
      binaryAbbreviationFirstLetter = 'G',
      decimalExponent = 9,
      binaryExponent = 30,
      isLarge = false
    ),
    createDataUnits(
      decimalPrefix = "Tera",
      binaryPrefix = "Tebi",
      decimalAbbreviationFirstLetter = 'T',
      binaryAbbreviationFirstLetter = 'T',
      decimalExponent = 12,
      binaryExponent = 40,
      isLarge = false
    ),
    createDataUnits(
      decimalPrefix = "Peta",
      binaryPrefix = "Pebi",
      decimalAbbreviationFirstLetter = 'P',
      binaryAbbreviationFirstLetter = 'P',
      decimalExponent = 15,
      binaryExponent = 50,
      isLarge = true
    ),
    createDataUnits(
      decimalPrefix = "Exa",
      binaryPrefix = "Exbi",
      decimalAbbreviationFirstLetter = 'E',
      binaryAbbreviationFirstLetter = 'E',
      decimalExponent = 18,
      binaryExponent = 60,
      isLarge = true
    ),
    createDataUnits(
      decimalPrefix = "Zetta",
      binaryPrefix = "Zebi",
      decimalAbbreviationFirstLetter = 'Z',
      binaryAbbreviationFirstLetter = 'Z',
      decimalExponent = 21,
      binaryExponent = 70,
      isLarge = true
    ),
    createDataUnits(
      decimalPrefix = "Yotta",
      binaryPrefix = "Yobi",
      decimalAbbreviationFirstLetter = 'Y',
      binaryAbbreviationFirstLetter = 'Y',
      decimalExponent = 24,
      binaryExponent = 80,
      isLarge = true
    )
  ).flatten()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createDataUnits(
    decimalPrefix: String,
    binaryPrefix: String,
    decimalAbbreviationFirstLetter: Char,
    binaryAbbreviationFirstLetter: Char,
    decimalExponent: Int,
    binaryExponent: Int,
    isLarge: Boolean
  ): List<DataUnit> = listOf(
    DataUnit("${decimalPrefix}bit", "${decimalAbbreviationFirstLetter}b", isLarge, BIT, DECIMAL, decimalExponent),
    DataUnit("${binaryPrefix}bit", "${decimalAbbreviationFirstLetter}ib", isLarge, BIT, BINARY, binaryExponent),
    DataUnit("${decimalPrefix}byte", "${binaryAbbreviationFirstLetter}B", isLarge, BYTE, DECIMAL, decimalExponent),
    DataUnit("${binaryPrefix}byte", "${binaryAbbreviationFirstLetter}iB", isLarge, BYTE, BINARY, binaryExponent)
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class NumberSystem(val title: String, val base: BigDecimal) {

    BASIC("Basic", BigDecimal.valueOf(2)),
    BINARY("Binary", BigDecimal.valueOf(2)),
    DECIMAL("Decimal", BigDecimal.valueOf(10))
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class BaseDataUnit {

    BIT,
    BYTE,
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  open class DataUnit(
    val name: String,
    val abbreviation: String,
    val isLarge: Boolean,
    val baseDataUnit: BaseDataUnit,
    val numberSystem: NumberSystem,
    private val exponent: Int
  ) {

    fun toBits(value: BigDecimal, mathContext: MathContext): BigDecimal {
      val conversationFactor = when (baseDataUnit) {
        BIT -> numberSystem.base.pow(exponent, mathContext)
        BYTE -> numberSystem.base.pow(exponent, mathContext).multiply(bigDecimalEight)
      }
      return value.multiply(conversationFactor, mathContext)
    }

    fun fromBits(value: BigDecimal, mathContext: MathContext): BigDecimal {
      val conversationFactor = when (baseDataUnit) {
        BIT -> numberSystem.base.pow(exponent, mathContext)
        BYTE -> numberSystem.base.pow(exponent, mathContext).multiply(bigDecimalEight)
      }
      return value.divide(conversationFactor, mathContext)
    }

    override fun toString(): String = name
  }
}
