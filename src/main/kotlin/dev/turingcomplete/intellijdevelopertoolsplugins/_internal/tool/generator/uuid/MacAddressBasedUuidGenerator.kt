package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.uuid

import com.fasterxml.uuid.EthernetAddress
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.asSequence
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.ValidationInfoBuilder
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.onSelected
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.toHexMacAddress
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.uuid.MacAddressBasedUuidGenerator.MacAddressGenerationMode.*
import java.net.NetworkInterface
import java.net.SocketException

abstract class MacAddressBasedUuidGenerator(
  version: UuidVersion,
  configuration: DeveloperToolConfiguration,
  private val parentDisposable: Disposable,
  supportsBulkGeneration: Boolean
) : SpecificUuidGenerator(supportsBulkGeneration) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var macAddressGenerationMode by configuration.register("${version}MacAddressGenerationMode", RANDOM)
  private var localInterface by configuration.register("${version}LocalInterface", "")
  private var individualMacAddress by configuration.register("${version}IndividualMacAddress", "")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildConfigurationUi(visible: ComponentPredicate) {
    buttonsGroup("MAC Address:") {
      row {
        radioButton("Generate random multicast MAC address")
                .configureMacAddressGenerationModeSelection(RANDOM)
      }

      row {
        val individualRadioButton = radioButton("Individual:")
                .configureMacAddressGenerationModeSelection(INDIVIDUAL)
                .gap(RightGap.SMALL)
        textField().text(individualMacAddress)
                .validation(validateIndividualMacAddress())
                .whenTextChangedFromUi(parentDisposable) { individualMacAddress = it }
                .enabledIf(individualRadioButton.selected).component
      }

      row {
        val localMacAddresses = collectLocalMacAddresses()
        visible(localMacAddresses.isNotEmpty())
        val useLocalInterface = radioButton("Local interface:")
                .configureMacAddressGenerationModeSelection(LOCAL_INTERFACE)
                .gap(RightGap.SMALL)
        comboBox(localMacAddresses)
                .applyToComponent {
                  model.asSequence().firstOrNull { it.macAddress.toHexMacAddress() == localInterface }?.let {
                    selectedItem = it
                  }
                }
                .whenItemSelectedFromUi { localInterface = it.macAddress.toHexMacAddress() }
                .enabledIf(useLocalInterface.selected).component
      }
    }.visibleIf(visible)
  }

  fun getEthernetAddress(): EthernetAddress = when (macAddressGenerationMode) {
    RANDOM -> EthernetAddress.constructMulticastAddress()
    INDIVIDUAL -> EthernetAddress.valueOf(individualMacAddress)
    LOCAL_INTERFACE -> EthernetAddress(localInterface)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun validateIndividualMacAddress(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? = {
    if (macAddressGenerationMode == INDIVIDUAL && !MAC_ADDRESS_REGEX.matches(individualMacAddress)) {
      INVALID_MAC_ADDRESS_VALIDATION_INFO
    }
    else {
      null
    }
  }

  private fun Cell<JBRadioButton>.configureMacAddressGenerationModeSelection(value: MacAddressGenerationMode) =
    this.applyToComponent {
      isSelected = macAddressGenerationMode == value
      onSelected { macAddressGenerationMode = value }
    }

  private fun collectLocalMacAddresses(): List<LocalInterface> {
    return try {
      NetworkInterface.getNetworkInterfaces().asSequence()
              .filter { !it.isLoopback }
              .filter { it.hardwareAddress != null }
              .filter { it.hardwareAddress.size == 6 }
              .map { LocalInterface(it.hardwareAddress, it.displayName) }
              .toList()
    }
    catch (ignore: SocketException) {
      emptyList()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class MacAddressGenerationMode {

    RANDOM,
    INDIVIDUAL,
    LOCAL_INTERFACE
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class LocalInterface(val macAddress: ByteArray, val title: String) {

    override fun toString(): String = "$title (${macAddress.toHexMacAddress()})"

    override fun equals(other: Any?): Boolean {
      if (this === other) {
        return true
      }
      if (javaClass != other?.javaClass) {
        return false
      }

      other as LocalInterface

      if (!macAddress.contentEquals(other.macAddress)) {
        return false
      }

      return true
    }

    override fun hashCode(): Int {
      return macAddress.contentHashCode()
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val MAC_ADDRESS_REGEX = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
    private val INVALID_MAC_ADDRESS_VALIDATION_INFO = ValidationInfo("Must be a valid MAC address")
  }
}