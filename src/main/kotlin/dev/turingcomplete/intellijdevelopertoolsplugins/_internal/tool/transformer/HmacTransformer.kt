package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import io.ktor.util.*
import org.bouncycastle.util.encoders.Hex
import java.security.Security
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


internal class HmacTransformer(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextTransformer(
  presentation = DeveloperToolContext(
    menuTitle = "HMAC",
    contentTitle = "HMAC Transformer"
  ),
  context = Context(
    transformActionTitle = "Generate",
    sourceTitle = "Data",
    resultTitle = "Hash"
  ),
  configuration = configuration,
  parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedAlgorithm = configuration.register("selectedAlgorithm", DEFAULT_ALGORITHM)

  private val secretKey: ObservableMutableProperty<String> = AtomicProperty("")

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    check(algorithms.isNotEmpty())

    secretKey.afterChange {
      if (!isDisposed && liveTransformation.get()) {
        transform()
      }
    }

    // Validate if selected algorithm is still available
    val selectedAlgorithm = selectedAlgorithm.get()
    if (algorithms.find { it.algorithm == selectedAlgorithm } == null) {
      this.selectedAlgorithm.set((algorithms.find { it.algorithm.equals(DEFAULT_ALGORITHM, true) } ?: algorithms.first()).algorithm)
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform() {
    if (validate().isNotEmpty()) {
      return
    }

    val hmac: ByteArray = Mac.getInstance(selectedAlgorithm.get()).run {
      init(SecretKeySpec(secretKey.get().encodeToByteArray(), selectedAlgorithm.get()))
      doFinal(sourceText.encodeToByteArray())
    }
    resultText = Hex.toHexString(hmac)
  }

  @Suppress("UnstableApiUsage")
  override fun Panel.buildTopConfigurationUi() {
    row {
      comboBox(algorithms)
        .label("Algorithm:")
        .applyToComponent { selectedItem = algorithms.find { it.algorithm == selectedAlgorithm.get() } }
        .whenItemSelectedFromUi { selectedAlgorithm.set(it.algorithm) }
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

  private data class HmacAlgorithm(val title: String, val algorithm: String) {

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool? {
      if (algorithms.isEmpty()) {
        return null
      }

      return HmacTransformer(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_ALGORITHM = "HMACSHA256"
    private val algorithms: List<HmacAlgorithm> by lazy {
      Security.getAlgorithms("Mac")
        .asSequence()
        .filter { it.startsWith("HMAC") }
        .filter {
          // Would require a complex PBEKey
          !it.contains("PBE")
        }
        .map { HmacAlgorithm(it.replace("HMAC", "Hmac"), it) }
        .sortedBy { it.title }
        .toList()
    }
  }
}