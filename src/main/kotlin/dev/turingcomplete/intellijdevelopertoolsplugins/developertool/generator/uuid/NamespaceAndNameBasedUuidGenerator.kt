package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.uuid

import com.fasterxml.uuid.Generators
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.uuid.NamespaceAndNameBasedUuidGenerator.NamespaceMode.INDIVIDUAL
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator.uuid.NamespaceAndNameBasedUuidGenerator.NamespaceMode.PREDEFINED
import java.awt.event.ItemEvent
import java.security.MessageDigest
import java.util.*

abstract class NamespaceAndNameBasedUuidGenerator(
        title: String,
        private val algorithm: MessageDigest,
        description: String? = null
) : UuidGenerator(title = title, description = description, supportsBulkGeneration = false) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var namespaceMode by createProperty("namespaceMode", PREDEFINED)
  private var predefinedNamespace by createProperty("predefinedNamespace", PredefinedNamespace.DNS)
  private var individualNamespace by createProperty("individualNamespace", "")
  private var name by createProperty("name", "")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildConfigurationUi(project: Project?, parentDisposable: Disposable) {
    buttonsGroup("Namespace:") {
      row {
        val usePredefined = radioButton("Predefined:").configure(PREDEFINED).gap(RightGap.SMALL)
        comboBox(PredefinedNamespace.values().toList()).configure().enabledIf(usePredefined.selected).component
      }

      row {
        val individualRadioButton = radioButton("Individual:").configure(INDIVIDUAL).gap(RightGap.SMALL)
        textField().text(individualNamespace).validation(validateIndividualNamespace())
                .whenTextChangedFromUi {
                  individualNamespace = it
                  doGenerate()
                }
                .enabledIf(individualRadioButton.selected).component
      }
    }

    row {
      textField().label("Name:").text(name).whenTextChangedFromUi {
        name = it
        doGenerate()
      }
    }
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

  private fun Cell<JBRadioButton>.configure(value: NamespaceMode): Cell<JBRadioButton> = this.apply {
    component.isSelected = namespaceMode == value
    component.addItemListener { event ->
      if (event.stateChange == ItemEvent.SELECTED) {
        namespaceMode = value
        doGenerate()
      }
    }
  }

  private fun Cell<ComboBox<PredefinedNamespace>>.configure(): Cell<ComboBox<PredefinedNamespace>> = this.apply {
    component.selectedItem = predefinedNamespace
    component.addItemListener { event ->
      if (event.stateChange == ItemEvent.SELECTED) {
        predefinedNamespace = component.selectedItem as PredefinedNamespace
        doGenerate()
      }
    }
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