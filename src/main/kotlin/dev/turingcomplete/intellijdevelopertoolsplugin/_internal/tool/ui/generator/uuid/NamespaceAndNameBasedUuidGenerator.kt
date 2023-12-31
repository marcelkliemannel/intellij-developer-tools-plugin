package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.generator.uuid

import com.fasterxml.uuid.Generators
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.builder.text
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.ValidationInfoBuilder
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.bind
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.generator.uuid.NamespaceAndNameBasedUuidGenerator.NamespaceMode.INDIVIDUAL
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.generator.uuid.NamespaceAndNameBasedUuidGenerator.NamespaceMode.PREDEFINED
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

  private var namespaceMode = configuration.register(
    "${version}NamespaceMode",
    PREDEFINED
  )
  private var predefinedNamespace = configuration.register(
    "${version}PredefinedNamespace",
    PredefinedNamespace.DNS
  )
  private var individualNamespace = configuration.register(
    "${version}IndividualNamespace",
    ""
  )
  private var name = configuration.register("${version}Name", "")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildConfigurationUi(visible: ComponentPredicate) {
    rowsRange {
      buttonsGroup("Namespace:") {
        row {
          val usePredefined = radioButton("Predefined:")
            .bind(namespaceMode, PREDEFINED)
            .gap(RightGap.SMALL)
          comboBox(PredefinedNamespace.entries)
            .bindItem(predefinedNamespace)
            .enabledIf(usePredefined.selected).component
        }

        row {
          val individualRadioButton = radioButton("Individual:")
            .bind(namespaceMode, INDIVIDUAL)
            .gap(RightGap.SMALL)
          textField()
            .text(individualNamespace.get())
            .validationInfo(validateIndividualNamespace())
            .whenTextChangedFromUi(parentDisposable) { individualNamespace.set(it) }
            .enabledIf(individualRadioButton.selected).component
        }
      }

      row {
        expandableTextField()
          .label("Name:")
          .bindText(name)
      }
    }.visibleIf(visible)
  }

  private fun validateIndividualNamespace(): ValidationInfoBuilder.(JBTextField) -> ValidationInfo? = {
    if (namespaceMode.get() == INDIVIDUAL && !UUID_REGEX.matches(it.text)) {
      ValidationInfo("Must be a valid UUID")
    }
    else {
      null
    }
  }

  final override fun generate(): String {
    val namespace: UUID = when (namespaceMode.get()) {
      PREDEFINED -> predefinedNamespace.get().value
      INDIVIDUAL -> UUID.fromString(individualNamespace.get())
    }

    return Generators.nameBasedGenerator(namespace, algorithm).generate(name.get()).toString()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
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