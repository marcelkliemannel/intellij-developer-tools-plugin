package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

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
import dev.turingcomplete.intellijdevelopertoolsplugin.common.decodeBase64String
import dev.turingcomplete.intellijdevelopertoolsplugin.common.emptyByteArray
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.SENSITIVE
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.generalSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.Companion.createSensitiveInputsHandlingToolTipText
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.SimpleToggleAction
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.registerDynamicToolTip
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.validateNonEmpty
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.TextInputOutputHandler.BytesToTextMode.BYTES_TO_HEX
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.UndirectionalConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.HmacTransformer.SecretKeyEncodingMode.BASE32
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.HmacTransformer.SecretKeyEncodingMode.BASE64
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.HmacTransformer.SecretKeyEncodingMode.RAW
import java.security.Security
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base32

class HmacTransformer(
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
    title = "HMAC",
    sourceTitle = "Data",
    targetTitle = "Hash",
    toTargetTitle = "Generate",
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var selectedAlgorithm = configuration.register("algorithm", DEFAULT_ALGORITHM)

  private val secretKey =
    configuration.register("secretKey", SECRET_KEY_DEFAULT, SENSITIVE, EXAMPLE_SECRET)
  private val secretKeyEncodingMode =
    configuration.register("secretKeyEncodingMode", RAW, CONFIGURATION)

  // -- Initialization ------------------------------------------------------ //

  init {
    check(hmacAlgorithms.isNotEmpty())

    // Validate if selected algorithm is still available
    val selectedAlgorithm = selectedAlgorithm.get()
    if (hmacAlgorithms.find { it.algorithm == selectedAlgorithm } == null) {
      this.selectedAlgorithm.set(
        (hmacAlgorithms.find { it.algorithm.equals(DEFAULT_ALGORITHM, true) }
            ?: hmacAlgorithms.first())
          .algorithm
      )
    }
  }

  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addTargetTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultTargetInputOutputHandlerId,
      bytesToTextMode = BYTES_TO_HEX,
    )
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray {
    val secretKeyValue = secretKey.get()
    if (secretKeyValue.isEmpty()) {
      return emptyByteArray
    }

    return Mac.getInstance(selectedAlgorithm.get()).run {
      val secretKey =
        when (secretKeyEncodingMode.get()) {
          RAW -> secretKeyValue
          BASE32 -> Base32().decode(secretKeyValue).decodeToString()
          BASE64 -> secretKeyValue.decodeBase64String()
        }
      init(SecretKeySpec(secretKey.encodeToByteArray(), selectedAlgorithm.get()))
      doFinal(source)
    }
  }

  override fun Panel.buildSourceTopConfigurationUi() {
    row {
      comboBox(hmacAlgorithms)
        .label("Algorithm:")
        .applyToComponent {
          selectedItem = hmacAlgorithms.find { it.algorithm == selectedAlgorithm.get() }
        }
        .whenItemSelectedFromUi { selectedAlgorithm.set(it.algorithm) }
    }
  }

  override fun Panel.buildSourceBottomConfigurationUi() {
    row {
      expandableTextField()
        .label("Secret key:")
        .align(AlignX.FILL)
        .bindText(secretKey)
        .validateNonEmpty("A secret key must be provided")
        .gap(RightGap.SMALL)
        .resizableColumn()
        .registerDynamicToolTip { generalSettings.createSensitiveInputsHandlingToolTipText() }

      val encodingActions =
        mutableListOf<AnAction>().apply {
          SecretKeyEncodingMode.entries.forEach { secretKeyEncodingModeValue ->
            add(
              SimpleToggleAction(
                text = secretKeyEncodingModeValue.title,
                icon = AllIcons.Actions.ToggleSoftWrap,
                isSelected = { secretKeyEncodingMode.get() == secretKeyEncodingModeValue },
                setSelected = { secretKeyEncodingMode.set(secretKeyEncodingModeValue) },
              )
            )
          }
        }
      actionButton(
        UiUtils.actionsPopup(
          title = "Encoding",
          icon = AllIcons.General.Settings,
          actions = encodingActions,
        )
      )
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  data class HmacAlgorithm(val title: String, val algorithm: String) {

    override fun toString(): String = title
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class SecretKeyEncodingMode(val title: String) {

    RAW("Raw"),
    BASE32("Base32 Encoded"),
    BASE64("Base64 Encoded"),
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<HmacTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "HMAC", contentTitle = "HMAC Transformer")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> HmacTransformer)? {
      if (hmacAlgorithms.isEmpty()) {
        return null
      }

      return { configuration -> HmacTransformer(context, configuration, parentDisposable, project) }
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val DEFAULT_ALGORITHM = "HMACSHA256"
    private const val SECRET_KEY_DEFAULT = ""

    val hmacAlgorithms: List<HmacAlgorithm> by lazy {
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
