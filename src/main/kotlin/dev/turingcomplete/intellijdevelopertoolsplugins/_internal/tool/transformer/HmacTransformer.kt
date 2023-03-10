package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import io.ktor.util.*
import org.bouncycastle.util.encoders.Hex
import java.security.Security
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


internal class HmacTransformer(
        configuration: DeveloperToolConfiguration,
        parentDisposable: Disposable
) : TextTransformer(
        presentation = DeveloperToolPresentation("HMAC", "HMAC Transformer"),
        transformActionTitle = "Generate",
        sourceTitle = "Data",
        resultTitle = "Hash",
        configuration = configuration,
        parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedAlgorithm: String by configuration.register("selectedAlgorithm", DEFAULT_ALGORITHM)

  private val secretKey: ObservableMutableProperty<String> = AtomicProperty("")

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    check(macAlgorithms.isNotEmpty())

    secretKey.afterChange {
      if (liveTransformation) {
       transform()
      }
    }

    // Validate if selected algorithm is still available
    if (macAlgorithms.find { it == selectedAlgorithm } == null) {
      selectedAlgorithm = macAlgorithms.find { it == DEFAULT_ALGORITHM } ?: macAlgorithms.first()
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform() {
    if (validate().isNotEmpty()) {
      return
    }

    val hmac: ByteArray = Mac.getInstance(selectedAlgorithm).run {
      init(SecretKeySpec(secretKey.get().encodeToByteArray(), selectedAlgorithm))
      doFinal(sourceText.encodeToByteArray())
    }
    resultText = Hex.toHexString(hmac)
  }

  @Suppress("UnstableApiUsage")
  override fun Panel.buildTopConfigurationUi() {
    row {
      comboBox(macAlgorithms)
              .label("Algorithm:")
              .applyToComponent { selectedItem = selectedAlgorithm }
              .whenItemSelectedFromUi { selectedAlgorithm = it }
    }
  }

  override fun Panel.buildMiddleConfigurationUi() {
    row {
      textField()
              .label("Secret key:")
              .bindText(secretKey)
              .columns(COLUMNS_LARGE)
              //.align(Align.FILL)
              .validation {
                if (it.text.isEmpty()) {
                  error("A secret key must be provided")
                }
                else {
                  null
                }
              }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool? {
      if (macAlgorithms.isEmpty()) {
        return null
      }

      return HmacTransformer(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_ALGORITHM = "HmacSHA256"
    private val macAlgorithms: List<String> by lazy {
      Security.getAlgorithms("Mac")
              .asSequence()
              .filter { it.startsWith("HMAC") }
              .filter {
                // Would require a complex PBEKey
                !it.contains("PBE")
              }
              .map { it.replace("HMAC", "Hmac") }
              .sorted()
              .toList()
    }
  }
}