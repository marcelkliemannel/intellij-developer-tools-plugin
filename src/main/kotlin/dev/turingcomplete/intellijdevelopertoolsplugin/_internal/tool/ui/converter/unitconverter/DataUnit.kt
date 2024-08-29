package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter

import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.BaseDataUnit.BIT
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.BaseDataUnit.BYTE
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.NumberSystem.BASE
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.NumberSystem.BINARY
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter.DataUnits.NumberSystem.DECIMAL
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.MathContext

internal object DataUnits {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val bigDecimalEight = BigDecimal.valueOf(8)

  val bitDataUnit = DataUnit(
    name = "Bit",
    abbreviation = "b",
    isLarge = false,
    baseDataUnit = BIT,
    numberSystem = BASE,
    power = 1,
    fixedConversationFactor = ONE
  )

  val dataUnits: List<DataUnit> = listOf(
    listOf(
      bitDataUnit,
      DataUnit(
        name = "Byte",
        abbreviation = "B",
        isLarge = false,
        baseDataUnit = BYTE,
        numberSystem = BASE,
        power = 3
      )
    ),
    createDataUnits(
      decimalPrefix = "Kilo",
      binaryPrefix = "Kibi",
      decimalAbbreviationFirstLetter = 'k',
      binaryAbbreviationFirstLetter = 'K',
      decimalPower = 3,
      binaryPower = 10,
      isLarge = false
    ),
    createDataUnits(
      decimalPrefix = "Mega",
      binaryPrefix = "Mebi",
      decimalAbbreviationFirstLetter = 'M',
      binaryAbbreviationFirstLetter = 'M',
      decimalPower = 6,
      binaryPower = 20,
      isLarge = false
    ),
    createDataUnits(
      decimalPrefix = "Giga",
      binaryPrefix = "Gibi",
      decimalAbbreviationFirstLetter = 'G',
      binaryAbbreviationFirstLetter = 'G',
      decimalPower = 9,
      binaryPower = 30,
      isLarge = false
    ),
    createDataUnits(
      decimalPrefix = "Tera",
      binaryPrefix = "Tebi",
      decimalAbbreviationFirstLetter = 'T',
      binaryAbbreviationFirstLetter = 'T',
      decimalPower = 12,
      binaryPower = 40,
      isLarge = false
    ),
    createDataUnits(
      decimalPrefix = "Peta",
      binaryPrefix = "Pebi",
      decimalAbbreviationFirstLetter = 'P',
      binaryAbbreviationFirstLetter = 'P',
      decimalPower = 15,
      binaryPower = 50,
      isLarge = true
    ),
    createDataUnits(
      decimalPrefix = "Exa",
      binaryPrefix = "Exbi",
      decimalAbbreviationFirstLetter = 'E',
      binaryAbbreviationFirstLetter = 'E',
      decimalPower = 18,
      binaryPower = 60,
      isLarge = true
    ),
    createDataUnits(
      decimalPrefix = "Zetta",
      binaryPrefix = "Zebi",
      decimalAbbreviationFirstLetter = 'Z',
      binaryAbbreviationFirstLetter = 'Z',
      decimalPower = 21,
      binaryPower = 70,
      isLarge = true
    ),
    createDataUnits(
      decimalPrefix = "Yotta",
      binaryPrefix = "Yobi",
      decimalAbbreviationFirstLetter = 'Y',
      binaryAbbreviationFirstLetter = 'Y',
      decimalPower = 24,
      binaryPower = 80,
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
    decimalPower: Int,
    binaryPower: Int,
    isLarge: Boolean
  ): List<DataUnit> = listOf(
    DataUnit("${decimalPrefix}bit", "${decimalAbbreviationFirstLetter}b", isLarge, BIT, DECIMAL, decimalPower),
    DataUnit("${binaryPrefix}bit", "${decimalAbbreviationFirstLetter}ib", isLarge, BIT, BINARY, binaryPower),
    DataUnit("${decimalPrefix}byte", "${binaryAbbreviationFirstLetter}B", isLarge, BYTE, DECIMAL, decimalPower),
    DataUnit("${binaryPrefix}byte", "${binaryAbbreviationFirstLetter}iB", isLarge, BYTE, BINARY, binaryPower)
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class NumberSystem(val title: String, val base: BigDecimal) {

    BASE("Base", BigDecimal.valueOf(2)),
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
    private val power: Int,
    private val fixedConversationFactor: BigDecimal? = null
  ) {

    fun conversationFactor(mathContext: MathContext): BigDecimal {
      fixedConversationFactor?.let { return it }

      return when (baseDataUnit) {
        BIT -> numberSystem.base.pow(power, mathContext).divide(bigDecimalEight, mathContext)
        BYTE -> numberSystem.base.pow(power, mathContext)
      }
    }

    override fun toString(): String = name
  }
}
