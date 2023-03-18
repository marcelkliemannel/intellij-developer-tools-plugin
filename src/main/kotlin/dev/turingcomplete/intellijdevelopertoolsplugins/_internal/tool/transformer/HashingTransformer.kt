package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.toMessageDigest
import org.bouncycastle.util.encoders.Hex
import java.security.Security

internal class HashingTransformer(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextTransformer(
  presentation = DeveloperToolContext(
    menuTitle = "Hashing",
    contentTitle = "Hashing Transformer"
  ),
  context = Context(
    transformActionTitle = "Hash",
    sourceTitle = "Plain",
    resultTitle = "Hashed"
  ),
  configuration = configuration,
  parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedAlgorithm = configuration.register("selectedAlgorithm", DEFAULT_ALGORITHM)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    check(messageDigestAlgorithms.isNotEmpty())

    // Validate if selected algorithm is still available
    if (messageDigestAlgorithms.find { it == selectedAlgorithm.get() } == null) {
      selectedAlgorithm.set(messageDigestAlgorithms.find { it == DEFAULT_ALGORITHM } ?: messageDigestAlgorithms.first())
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform() {
    val hash = selectedAlgorithm.get().toMessageDigest().digest(sourceText.encodeToByteArray())
    resultText = Hex.encode(hash).decodeToString()
  }

  override fun Panel.buildTopConfigurationUi() {
    row {
      comboBox(messageDigestAlgorithms)
        .label("Algorithm:")
        .bindItem(selectedAlgorithm)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory {
    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool? {
      if (messageDigestAlgorithms.isEmpty()) {
        return null
      }

      return HashingTransformer(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_ALGORITHM = "SHA-256"
    private val messageDigestAlgorithms: List<String> by lazy { Security.getAlgorithms("MessageDigest").sorted() }
  }
}