package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import at.favre.lib.crypto.bcrypt.BCrypt
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.columns
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.SENSITIVE
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.UndirectionalConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

class BCryptTransformer(
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
    title = UiToolsBundle.message("bcrypt-transformer.title"),
    sourceTitle = UiToolsBundle.message("bcrypt-transformer.source-title"),
    targetTitle = UiToolsBundle.message("bcrypt-transformer.target-title"),
    toTargetTitle = UiToolsBundle.message("bcrypt-transformer.hash"),
  ) {
  // -- Properties ---------------------------------------------------------- //

  private val cost = configuration.register("cost", DEFAULT_COST)
  private val selectedVersion = configuration.register("version", DEFAULT_VERSION)

  /**
   * BCrypt is a deliberately slow key derivation function. Even the default cost takes a noticeable
   * amount of time and the maximum cost takes minutes, which makes it unsuitable to be executed on
   * every keystroke. Therefore, the hash is only calculated on demand in a non-cancelable
   * background task because the BCrypt library does not support interrupting an active hash.
   */
  override val liveConversionPossible: Boolean = false

  override val backgroundConversionCancellable: Boolean = false

  override val toTargetProgressTitle: String =
    UiToolsBundle.message("bcrypt-transformer.hashing-title")

  override val toTargetProgressDescription: String
    get() = UiToolsBundle.message("bcrypt-transformer.hashing-progress", cost.get())

  // -- Initialization ------------------------------------------------------ //

  init {
    if (selectedVersion.get() !in BCRYPT_VERSIONS) {
      selectedVersion.set(DEFAULT_VERSION)
    }
  }

  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultSourceInputOutputHandlerId,
      exampleText = EXAMPLE_PASSWORD,
      propertyType = SENSITIVE,
    )
  }

  /**
   * `BCrypt.withDefaults()` uses the strict long password strategy, which rejects passwords longer
   * than 72 bytes instead of silently truncating them (a truncated password would produce a hash
   * that other passwords match too). That error, like an out-of-range cost, gets surfaced by the
   * `errorHolder` of the source input.
   */
  override fun doConvertToTarget(source: ByteArray): ByteArray =
    BCrypt.with(selectedVersion.get().toBcryptVersion()).hash(cost.get(), source)

  override fun Panel.buildSourceTopConfigurationUi() {
    row {
        textField()
          .label(UiToolsBundle.message("bcrypt-transformer.cost"))
          .bindIntTextImproved(cost)
          .columns(4)
          .validateLongValue(
            LongRange(BCrypt.MIN_COST.toLong(), BCrypt.MAX_COST.toLong()),
            warning = { value ->
              if (value > COST_WARNING_THRESHOLD) {
                UiToolsBundle.message("bcrypt-transformer.cost-warning", value)
              } else {
                null
              }
            },
          )
          .gap(RightGap.SMALL)
        contextHelp(UiToolsBundle.message("bcrypt-transformer.cost-help"))
      }
      .layout(RowLayout.PARENT_GRID)
  }

  override fun Panel.buildBottomConfigurationUi() {
    collapsibleGroup(UiToolsBundle.message("bcrypt-transformer.advanced-settings"), false) {
      row {
        comboBox(BCRYPT_VERSIONS)
          .label(UiToolsBundle.message("bcrypt-transformer.version"))
          .bindItem(selectedVersion)
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun String.toBcryptVersion(): BCrypt.Version =
    when (this) {
      VERSION_2B -> BCrypt.Version.VERSION_2B
      VERSION_2Y -> BCrypt.Version.VERSION_2Y
      else -> BCrypt.Version.VERSION_2A
    }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<BCryptTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("bcrypt-transformer.menu-title"),
        contentTitle = UiToolsBundle.message("bcrypt-transformer.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> BCryptTransformer) = { configuration ->
      BCryptTransformer(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val DEFAULT_COST = 10
    private const val COST_WARNING_THRESHOLD = 16
    private const val DEFAULT_VERSION = "\$2a\$"
    private const val VERSION_2B = "\$2b\$"
    private const val VERSION_2Y = "\$2y\$"
    private const val EXAMPLE_PASSWORD = "s3cr3t-p4ssw0rd"

    private val BCRYPT_VERSIONS = listOf(DEFAULT_VERSION, VERSION_2B, VERSION_2Y)
  }
}
