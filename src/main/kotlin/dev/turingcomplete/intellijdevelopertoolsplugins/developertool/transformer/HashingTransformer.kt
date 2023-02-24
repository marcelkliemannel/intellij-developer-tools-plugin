package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.GeneralDeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.onChanged
import org.bouncycastle.util.encoders.Hex
import java.security.MessageDigest

class HashingTransformer : TextTransformer(
        id = "hashing",
        title = "Hashing",
        transformActionTitle = "Hash",
        sourceTitle = "Plain",
        resultTitle = "Hashed"
), GeneralDeveloperTool {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var algorithm: HashAlgorithm by createProperty("algorithm", HashAlgorithm.Sha256Hash)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform(text: String): String {
    val hash = algorithm.messageDigest.digest(text.encodeToByteArray())
    return Hex.encode(hash).decodeToString()
  }

  override fun Panel.buildConfigurationUi(project: Project?, parentDisposable: Disposable) {
    row {
      label("Algorithm:").gap(RightGap.SMALL)
      comboBox(HashAlgorithm.values().toList()).configure()
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Cell<ComboBox<HashAlgorithm>>.configure() = this.applyToComponent {
    selectedItem = algorithm
    onChanged { algorithm = it }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class HashAlgorithm(val messageDigest: MessageDigest) {
    Md2Hash("MD2"),
    Md4Hash("MD4"),
    Md5Hash("MD5"),
    Sha1Hash("SHA-1"),
    Sha256Hash("SHA-256"),
    Sha384Hash("SHA-384"),
    Sha512Hash("SHA-512"),
    ShaThree224Hash("SHA3-224"),
    ShaThree256Hash("SHA3-256"),
    ShaThree384Hash("SHA3-384"),
    ShaThree512Hash("SHA3-512"),
    TigerHash("Tiger"),
    WhirlpoolHash("Whirlpool"),
    Ripemd128Hash("RIPEMD128"),
    Ripemd160Hash("RIPEMD160"),
    Ripemd256Hash("RIPEMD256"),
    Ripemd320Hash("RIPEMD320");

    constructor(algorithm: String) : this(MessageDigest.getInstance(algorithm))

    override fun toString(): String = messageDigest.algorithm
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}