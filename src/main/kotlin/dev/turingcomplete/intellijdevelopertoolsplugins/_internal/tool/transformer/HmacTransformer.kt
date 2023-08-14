package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.SECRET
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.toHexString
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.validateNonEmpty
import io.ktor.util.*
import java.security.Security
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class HmacTransformer(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextTransformer(
  textTransformerContext = TextTransformerContext(
    transformActionTitle = "Generate",
    sourceTitle = "Data",
    resultTitle = "Hash"
  ),
  configuration = configuration,
  parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedAlgorithm = configuration.register("algorithm", DEFAULT_ALGORITHM)

  private val secretKey = configuration.register("secretKey", SECRET_KEY_DEFAULT, SECRET, EXAMPLE_SECRET)
  private val secretKeyBase64Encoded = configuration.register("secretKeyBase64Encoded", SECRET_KEY_BASE64_ENCODED_DEFAULT, CONFIGURATION)

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
      val secretKey = if (secretKeyBase64Encoded.get()) {
        secretKey.get().decodeBase64String()
      }
      else {
        secretKey.get()
      }

      init(SecretKeySpec(secretKey.encodeToByteArray(), selectedAlgorithm.get()))
      doFinal(sourceText.get().encodeToByteArray())
    }
    resultText.set(hmac.toHexString())
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
        .validateNonEmpty("A secret key must be provided")

      checkBox("Secret key is Base64 encoded")
        .bindSelected(secretKeyBase64Encoded)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private data class HmacAlgorithm(val title: String, val algorithm: String) {

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<HmacTransformer> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "HMAC",
      contentTitle = "HMAC Transformer"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> HmacTransformer)? {
      if (algorithms.isEmpty()) {
        return null
      }

      return { configuration -> HmacTransformer(configuration, parentDisposable) }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_ALGORITHM = "HMACSHA256"
    private const val SECRET_KEY_DEFAULT = ""
    private const val SECRET_KEY_BASE64_ENCODED_DEFAULT = false

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

    private const val EXAMPLE_SECRET = "s3cre!"
  }
}