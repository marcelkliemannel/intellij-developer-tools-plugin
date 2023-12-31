package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolExContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.toHexString
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.toMessageDigest
import java.security.Security

internal class HashingTransformer(
  context: DeveloperUiToolExContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  project: Project?
) : TextTransformer(
  textTransformerContext = TextTransformerContext(
    transformActionTitle = "Hash",
    sourceTitle = "Plain",
    resultTitle = "Hashed"
  ),
  context = context,
  configuration = configuration,
  parentDisposable = parentDisposable,
  project = project
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedAlgorithm = configuration.register("algorithm", DEFAULT_ALGORITHM)

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
    val hash = selectedAlgorithm.get().toMessageDigest().digest(sourceText.get().encodeToByteArray())
    resultText.set(hash.toHexString())
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

  class Factory : DeveloperUiToolFactory<HashingTransformer> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Hashing",
      contentTitle = "Hashing Transformer"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolExContext
    ): ((DeveloperToolConfiguration) -> HashingTransformer)? {
      if (messageDigestAlgorithms.isEmpty()) {
        return null
      }

      return { configuration -> HashingTransformer(context, configuration, parentDisposable, project) }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_ALGORITHM = "SHA-256"
    private val messageDigestAlgorithms: List<String> by lazy { Security.getAlgorithms("MessageDigest").sorted() }
  }
}