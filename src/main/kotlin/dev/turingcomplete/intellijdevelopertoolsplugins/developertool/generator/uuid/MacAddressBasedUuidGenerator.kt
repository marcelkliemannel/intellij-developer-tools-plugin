package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.uuid

import com.fasterxml.uuid.EthernetAddress
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.asSequence
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.uuid.MacAddressBasedUuidGenerator.MacAddressGenerationMode.*
import dev.turingcomplete.intellijdevelopertoolsplugins.onChanged
import dev.turingcomplete.intellijdevelopertoolsplugins.onSelected
import dev.turingcomplete.intellijdevelopertoolsplugins.toHexMacAddress
import java.net.NetworkInterface
import java.net.SocketException

abstract class MacAddressBasedUuidGenerator(title: String, description: String? = null)
  : UuidGenerator(title = title, description = description) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var macAddressGenerationMode by createProperty("macAddressGenerationMode", RANDOM)
  private var localInterface by createProperty("localInterface", "")
  private var individualMacAddress by createProperty("individualMacAddress", "")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildConfigurationUi(project: Project?, parentDisposable: Disposable) {
    buttonsGroup("MAC Address:") {
      row {
        radioButton("Generate random multicast MAC address").configure(RANDOM)
      }

      row {
        val individualRadioButton = radioButton("Individual:").configure(INDIVIDUAL).gap(RightGap.SMALL)
        @Suppress("UnstableApiUsage")
        textField().text(individualMacAddress)
                .validation(validateIndividualMacAddress())
                .whenTextChangedFromUi(parentDisposable) { individualMacAddress = it }
                .enabledIf(individualRadioButton.selected).component
      }

      row {
        val localMacAddresses = collectLocalMacAddresses()
        visible(localMacAddresses.isNotEmpty())
        val useLocalInterface = radioButton("Local interface:").configure(LOCAL_INTERFACE).gap(RightGap.SMALL)
        comboBox(localMacAddresses).configure().enabledIf(useLocalInterface.selected).component
      }.bottomGap(BottomGap.MEDIUM)
    }
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

  private fun Cell<JBRadioButton>.configure(value: MacAddressGenerationMode) = this.applyToComponent {
    isSelected = macAddressGenerationMode == value
    onSelected { macAddressGenerationMode = value }
  }

  private fun Cell<ComboBox<LocalInterface>>.configure() = this.applyToComponent {
    model.asSequence().firstOrNull { it.macAddress.toHexMacAddress() == localInterface }?.let {
      component.selectedItem = it
    }
    onChanged { localInterface = it.macAddress.toHexMacAddress() }
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