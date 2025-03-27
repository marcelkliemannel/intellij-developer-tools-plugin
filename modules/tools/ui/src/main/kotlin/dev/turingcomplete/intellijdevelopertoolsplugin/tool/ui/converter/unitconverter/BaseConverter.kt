package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.unitconverter

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.not
import java.math.BigInteger

class BaseConverter(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  UnitConverter(parentDisposable, "Base") {
  // -- Properties ---------------------------------------------------------- //

  private val baseTwoInput =
    configuration.register(
      key = "${CONFIGURATION_KEY_PREFIX}baseTwoInput",
      defaultValue = "0",
      propertyType = INPUT,
      example = EXAMPLE_BASE_TWO_INPUT,
    )
  private val showOnlyCommonBases =
    configuration.register("${CONFIGURATION_KEY_PREFIX}showOnlyCommonBases", true, CONFIGURATION)

  private lateinit var baseProperties: Map<Int, ValueProperty<String>>

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    baseProperties =
      bases
        .map { (base, title) ->
          val property = if (base == 2) baseTwoInput else ValueProperty("0")
          row {
              lateinit var textField: JBTextField
              val textFieldCell =
                textField()
                  .validate(base)
                  .label("$title ($base):")
                  .bindText(property)
                  .whenTextChangedFromUi { convertFromCommonBase(base, textField) }
                  .resizableColumn()
                  .align(Align.FILL)
                  .gap(RightGap.SMALL)
              if (!commonBases.contains(base)) {
                textFieldCell.visibleIf(showOnlyCommonBases.not())
              }
              textField = textFieldCell.component
            }
            .layout(RowLayout.PARENT_GRID)

          base to property
        }
        .toMap()
  }

  override fun Panel.buildSettingsUi() {
    collapsibleGroup("Settings") {
        row { checkBox("Show only common bases").bindSelected(showOnlyCommonBases) }
      }
      .topGap(TopGap.NONE)
  }

  override fun sync() {
    convertFromCommonBase(2, null)
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun convertFromCommonBase(inputBase: Int, textField: JBTextField?) {
    if (textField != null && validate().any { it.component == textField }) {
      return
    }

    val inputProperty =
      baseProperties[inputBase] ?: throw IllegalArgumentException("Unknown base: $inputBase")
    val input = inputProperty.get().parse(inputBase) ?: return

    baseProperties
      .filter { it.value != inputProperty }
      .forEach { (base, property) -> property.set(input.toString(base).uppercase()) }
  }

  @Suppress("UnstableApiUsage")
  private fun Cell<JBTextField>.validate(base: Int) =
    this.apply {
      validationInfo {
        if (!this@validate.component.isEnabled) {
          return@validationInfo null
        }
        try {
          this@validate.component.text.parse(base)
            ?: return@validationInfo error("Please enter a number")
          return@validationInfo null
        } catch (_: Exception) {
          return@validationInfo error("Please enter a valid number")
        }
      }
    }

  private fun String.parse(base: Int): BigInteger? {
    if (this.isBlank()) {
      return null
    }

    val maxAllowedChar = validChars[base - 1]
    val isValid = this.uppercase().all { it in '0'..maxAllowedChar }
    if (!isValid) {
      throw IllegalArgumentException("Invalid input for base $base")
    }

    return BigInteger(this, base)
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val CONFIGURATION_KEY_PREFIX = "baseConverter_"
    private const val EXAMPLE_BASE_TWO_INPUT = "10101010"

    private val validChars = ('0'..'9') + ('A'..'Z')

    private val commonBases = setOf(2, 8, 10, 16)
    private val bases =
      linkedMapOf(
        2 to "Binary",
        3 to "Ternary",
        4 to "Quaternary",
        5 to "Quinary",
        6 to "Senary",
        7 to "Septenary",
        8 to "Octal",
        9 to "Nonary",
        10 to "Decimal",
        11 to "Undecimal",
        12 to "Duodecimal",
        13 to "Tridecimal",
        14 to "Tetradecimal",
        15 to "Pentadecimal",
        16 to "Hexadecimal",
        17 to "Heptadecimal",
        18 to "Octodecimal",
        19 to "Enneadecimal",
        20 to "Vigesimal",
        21 to "Unvigesimal",
        22 to "Duovigesimal",
        23 to "Trivigesimal",
        24 to "Tetravigesimal",
        25 to "Pentavigesimal",
        26 to "Hexavigesimal",
        27 to "Septemvigesimal",
        28 to "Octovigesimal",
        29 to "Ennevigesimal",
        30 to "Trigesimal",
        31 to "Untrigesimal",
        32 to "Duotrigesimal",
        33 to "Tretrigesimal",
        34 to "Tetratrigesimal",
        35 to "Pentatrigesimal",
        36 to "Hexatrigesimal",
      )
  }
}
