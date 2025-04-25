package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugin.common.toMessageDigest
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.TextInputOutputHandler.BytesToTextMode.BYTES_TO_HEX
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.UndirectionalConverter
import java.security.Security

class HashingTransformer(
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  project: Project?,
) :
  UndirectionalConverter(
    context = context,
    configuration = configuration,
    parentDisposable = parentDisposable,
    project = project,
    title = "Hashing",
    sourceTitle = "Plain",
    targetTitle = "Hashed",
    toTargetTitle = "Hash",
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var selectedAlgorithm = configuration.register("algorithm", DEFAULT_ALGORITHM)

  // -- Initialization ------------------------------------------------------ //

  init {
    check(messageDigestAlgorithms.isNotEmpty())

    // Validate if selected algorithm is still available
    if (messageDigestAlgorithms.find { it == selectedAlgorithm.get() } == null) {
      selectedAlgorithm.set(
        messageDigestAlgorithms.find { it == DEFAULT_ALGORITHM } ?: messageDigestAlgorithms.first()
      )
    }
  }

  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultSourceInputOutputHandlerId,
      bytesToTextMode = BYTES_TO_HEX,
    )
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    selectedAlgorithm.get().toMessageDigest().digest(source)

  override fun Panel.buildSourceTopConfigurationUi() {
    row { comboBox(messageDigestAlgorithms).label("Algorithm:").bindItem(selectedAlgorithm) }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<HashingTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "Hashing", contentTitle = "Hashing Transformer")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> HashingTransformer)? {
      if (messageDigestAlgorithms.isEmpty()) {
        return null
      }

      return { configuration ->
        HashingTransformer(context, configuration, parentDisposable, project)
      }
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val DEFAULT_ALGORITHM = "SHA-256"
    private val messageDigestAlgorithms: List<String> by lazy {
      Security.getAlgorithms("MessageDigest").sorted()
    }
  }
}
