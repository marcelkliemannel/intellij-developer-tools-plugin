package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import org.bouncycastle.util.encoders.Hex
import java.security.MessageDigest

internal class HashingTransformer(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextTransformer(
    presentation = DeveloperToolPresentation("Hashing", "Hashing Transformer"),
    transformActionTitle = "Hash",
    sourceTitle = "Plain",
    resultTitle = "Hashed",
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedAlgorithm: HashAlgorithm by configuration.register("selectedAlgorithm", HashAlgorithm.SHA_256)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform() {
    val hash = selectedAlgorithm.messageDigest.digest(sourceText.encodeToByteArray())
    resultText = Hex.encode(hash).decodeToString()
  }

  @Suppress("UnstableApiUsage")
  override fun Panel.buildConfigurationUi() {
    row {
      comboBox(HashAlgorithm.values().toList())
              .label("Algorithm:")
              .applyToComponent { selectedItem = selectedAlgorithm }
              .whenItemSelectedFromUi { selectedAlgorithm = it }
    }
  }

  override fun configurationPosition() = ConfigurationPosition.TOP

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class HashAlgorithm(val messageDigest: MessageDigest) {
    MD2("MD2"),
    MD4("MD4"),
    MD5("MD5"),
    SHA_1("SHA-1"),
    SHA_256("SHA-256"),
    SHA_384("SHA-384"),
    SHA_512("SHA-512"),
    SHA3_224("SHA3-224"),
    SHA3_256("SHA3-256"),
    SHA3_384("SHA3-384"),
    SHA3_512("SHA3-512"),
    TIGER("Tiger"),
    WHIRLPOOL("Whirlpool"),
    RIPEMD_128("RIPEMD128"),
    RIPEMD_160("RIPEMD160"),
    RIPEMD_256("RIPEMD256"),
    RIPEMD_320("RIPEMD320");

    constructor(algorithm: String) : this(MessageDigest.getInstance(algorithm))

    override fun toString(): String = messageDigest.algorithm
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory {
    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return HashingTransformer(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}