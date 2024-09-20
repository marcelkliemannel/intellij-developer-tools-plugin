package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.SENSITIVE
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.SimpleToggleAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.UiUtils
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.registerDynamicToolTip
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.toHexString
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.validateNonEmpty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer.HmacTransformer.SecretKeyEncodingMode.BASE32
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer.HmacTransformer.SecretKeyEncodingMode.BASE64
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer.HmacTransformer.SecretKeyEncodingMode.RAW
import io.ktor.util.*
import org.apache.commons.codec.binary.Base32
import java.security.Security
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class HmacTransformer(
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  project: Project?
) : TextTransformer(
  textTransformerContext = TextTransformerContext(
    transformActionTitle = "Generate",
    sourceTitle = "Data",
    resultTitle = "Hash"
  ),
  context = context,
  configuration = configuration,
  parentDisposable = parentDisposable,
  project = project
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedAlgorithm = configuration.register("algorithm", DEFAULT_ALGORITHM)

  private val secretKey = configuration.register("secretKey", SECRET_KEY_DEFAULT, SENSITIVE, EXAMPLE_SECRET)
  private val secretKeyEncodingMode = configuration.register("secretKeyEncodingMode", RAW, CONFIGURATION)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    check(algorithms.isNotEmpty())

    secretKey.afterChange {
      if (!isDisposed && liveTransformation.get()) {
        transform()
      }
    }

    secretKeyEncodingMode.afterChange {
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

    val secretKeyValue = secretKey.get()
    if (secretKeyValue.isEmpty()) {
      resultText.set("")
      return
    }

    val hmac: ByteArray = Mac.getInstance(selectedAlgorithm.get()).run {
      val secretKey = when (secretKeyEncodingMode.get()) {
        RAW -> secretKeyValue
        BASE32 -> Base32().decode(secretKeyValue).decodeToString()
        BASE64 -> secretKeyValue.decodeBase64String()
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
      expandableTextField()
        .label("Secret key:")
        .align(AlignX.FILL)
        .bindText(secretKey)
        .validateNonEmpty("A secret key must be provided")
        .gap(RightGap.SMALL)
        .resizableColumn()
        .registerDynamicToolTip({ DeveloperToolsApplicationSettings.instance.createSensitiveInputsHandlingToolTipText() })

      val encodingActions = mutableListOf<AnAction>().apply {
        SecretKeyEncodingMode.entries.forEach { secretKeyEncodingModeValue ->
          add(SimpleToggleAction(
            text = secretKeyEncodingModeValue.title,
            icon = AllIcons.Actions.ToggleSoftWrap,
            isSelected = { secretKeyEncodingMode.get() == secretKeyEncodingModeValue },
            setSelected = {
              secretKeyEncodingMode.set(secretKeyEncodingModeValue)
            }
          ))
        }
      }
      actionButton(UiUtils.actionsPopup(
        title = "Encoding",
        icon = AllIcons.General.Settings,
        actions = encodingActions
      ))
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private data class HmacAlgorithm(val title: String, val algorithm: String) {

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class SecretKeyEncodingMode(val title: String) {

    RAW("Raw"),
    BASE32("Base32 Encoded"),
    BASE64("Base64 Encoded")
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<HmacTransformer> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "HMAC",
      contentTitle = "HMAC Transformer"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> HmacTransformer)? {
      if (algorithms.isEmpty()) {
        return null
      }

      return { configuration -> HmacTransformer(context, configuration, parentDisposable, project) }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_ALGORITHM = "HMACSHA256"
    private const val SECRET_KEY_DEFAULT = ""

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