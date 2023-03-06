package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.uuid

import com.fasterxml.uuid.Generators
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.ValidationInfoBuilder
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.onChanged
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.onSelected
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.uuid.NamespaceAndNameBasedUuidGenerator.NamespaceMode.INDIVIDUAL
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.uuid.NamespaceAndNameBasedUuidGenerator.NamespaceMode.PREDEFINED
import java.security.MessageDigest
import java.util.*

abstract class NamespaceAndNameBasedUuidGenerator(
        version: UuidVersion,
        private val algorithm: MessageDigest,
        configuration: DeveloperToolConfiguration,
        private val parentDisposable: Disposable,
        supportsBulkGeneration: Boolean
) : SpecificUuidGenerator(supportsBulkGeneration) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var namespaceMode by configuration.register("${version}NamespaceMode", PREDEFINED)
  private var predefinedNamespace by configuration.register("${version}PredefinedNamespace", PredefinedNamespace.DNS)
  private var individualNamespace by configuration.register("${version}IndividualNamespace", "")
  private var name by configuration.register("${version}Name", "")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildConfigurationUi(visible: ComponentPredicate) {
    rowsRange {
      buttonsGroup("Namespace:") {
        row {
          val usePredefined = radioButton("Predefined:").configure(PREDEFINED).gap(RightGap.SMALL)
          comboBox(PredefinedNamespace.values().toList()).configure().enabledIf(usePredefined.selected).component
        }

        row {
          val individualRadioButton = radioButton("Individual:").configure(INDIVIDUAL).gap(RightGap.SMALL)
          textField().text(individualNamespace).validation(validateIndividualNamespace())
                  .whenTextChangedFromUi(parentDisposable) { individualNamespace = it }
                  .enabledIf(individualRadioButton.selected).component
        }
      }

      row {
        textField().label("Name:")
                .text(name)
                .whenTextChangedFromUi(parentDisposable) { name = it }
      }
    }.visibleIf(visible)
  }

  private fun validateIndividualNamespace(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? = {
    if (namespaceMode == INDIVIDUAL && !UUID_REGEX.matches(it.text)) {
      ValidationInfo("Must be a valid UUID")
    }
    else {
      null
    }
  }

  final override fun generate(): String {
    val namespace: UUID = when (namespaceMode) {
      PREDEFINED -> predefinedNamespace.value
      INDIVIDUAL -> UUID.fromString(individualNamespace)
    }

    return Generators.nameBasedGenerator(namespace, algorithm).generate(name).toString()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Cell<JBRadioButton>.configure(value: NamespaceMode) = this.applyToComponent {
    isSelected = namespaceMode == value
    onSelected { namespaceMode = value }
  }

  private fun Cell<ComboBox<PredefinedNamespace>>.configure() = this.applyToComponent {
    selectedItem = predefinedNamespace
    onChanged { predefinedNamespace = it }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class NamespaceMode {

    PREDEFINED,
    INDIVIDUAL
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class PredefinedNamespace(private val title: String, val value: UUID) {

    DNS("DNS", UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")),
    URL("URL", UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")),
    OID("OID", UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8")),
    X500DN("X.500 DN", UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8"));

    override fun toString(): String = "$title ($value)"
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\$")
  }
}