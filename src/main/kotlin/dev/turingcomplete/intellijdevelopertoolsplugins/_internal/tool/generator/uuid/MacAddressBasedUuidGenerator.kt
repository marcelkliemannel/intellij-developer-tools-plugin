package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.uuid

import com.fasterxml.uuid.EthernetAddress
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.asSequence
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.ValidationInfoBuilder
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.bind
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.toHexMacAddress
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.uuid.MacAddressBasedUuidGenerator.MacAddressGenerationMode.INDIVIDUAL
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.uuid.MacAddressBasedUuidGenerator.MacAddressGenerationMode.LOCAL_INTERFACE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.uuid.MacAddressBasedUuidGenerator.MacAddressGenerationMode.RANDOM
import java.net.NetworkInterface
import java.net.SocketException

abstract class MacAddressBasedUuidGenerator(
  version: UuidVersion,
  configuration: DeveloperToolConfiguration,
  supportsBulkGeneration: Boolean
) : SpecificUuidGenerator(supportsBulkGeneration) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var macAddressGenerationMode = configuration.register(
    "${version}MacAddressGenerationMode",
    RANDOM
  )
  private var localInterface by configuration.register(
    "${version}LocalInterface",
    ""
  )
  private var individualMacAddress = configuration.register(
    "${version}IndividualMacAddress",
    ""
  )

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildConfigurationUi(visible: ComponentPredicate) {
    buttonsGroup("MAC Address:") {
      row {
        radioButton("Generate random multicast MAC address")
          .bind(macAddressGenerationMode, RANDOM)
      }

      row {
        val individualRadioButton = radioButton("Individual:")
          .bind(macAddressGenerationMode, INDIVIDUAL)
          .gap(RightGap.SMALL)
        expandableTextField()
          .bindText(individualMacAddress)
          .validationInfo(validateIndividualMacAddress())
          .enabledIf(individualRadioButton.selected).component
      }

      row {
        val localMacAddresses = collectLocalMacAddresses()
        visible(localMacAddresses.isNotEmpty())
        val useLocalInterface = radioButton("Local interface:")
          .bind(macAddressGenerationMode, LOCAL_INTERFACE)
          .gap(RightGap.SMALL)
        comboBox(localMacAddresses)
          .applyToComponent {
            model.asSequence().firstOrNull { it.macAddress == localInterface }?.let {
              selectedItem = it
            }
          }
          .whenItemSelectedFromUi { localInterface = it.macAddress }
          .enabledIf(useLocalInterface.selected).component
      }
    }.visibleIf(visible)
  }

  fun getEthernetAddress(): EthernetAddress = when (macAddressGenerationMode.get()) {
    RANDOM -> EthernetAddress.constructMulticastAddress()
    INDIVIDUAL -> EthernetAddress.valueOf(individualMacAddress.get())
    LOCAL_INTERFACE -> EthernetAddress(localInterface)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun validateIndividualMacAddress(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? = {
    if (macAddressGenerationMode.get() == INDIVIDUAL && !MAC_ADDRESS_REGEX.matches(individualMacAddress.get())) {
      INVALID_MAC_ADDRESS_VALIDATION_INFO
    }
    else {
      null
    }
  }

  private fun collectLocalMacAddresses(): List<LocalInterface> {
    return try {
      NetworkInterface.getNetworkInterfaces().asSequence()
        .filter { !it.isLoopback }
        .filter { it.hardwareAddress != null }
        .filter { it.hardwareAddress.size == 6 }
        .groupBy { it.hardwareAddress.toHexMacAddress() }
        .map { LocalInterface(it.key, it.value.joinToString("/") { networkInterface -> networkInterface.name }) }
        .toList()
    } catch (ignore: SocketException) {
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

  private class LocalInterface(val macAddress: String, val title: String) {

    override fun toString(): String = "$title (${macAddress})"

    override fun equals(other: Any?): Boolean {
      if (this === other) {
        return true
      }
      if (javaClass != other?.javaClass) {
        return false
      }

      other as LocalInterface

      return macAddress == other.macAddress
    }

    override fun hashCode(): Int = macAddress.hashCode()
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val MAC_ADDRESS_REGEX = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
    private val INVALID_MAC_ADDRESS_VALIDATION_INFO = ValidationInfo("Must be a valid MAC address")
  }
}