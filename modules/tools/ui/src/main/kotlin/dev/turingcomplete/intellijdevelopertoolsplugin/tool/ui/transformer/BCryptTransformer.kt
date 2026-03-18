package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import at.favre.lib.crypto.bcrypt.BCrypt
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
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

  /**
   * BCrypt is a deliberately slow key derivation function. Even the default cost takes a noticeable
   * amount of time and the maximum cost takes minutes, which makes it unsuitable to be executed on
   * every keystroke. Therefore, the hash is only calculated on demand, as a cancelable background
   * task.
   */
  override val liveConversionPossible: Boolean = false

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultSourceInputOutputHandlerId,
      exampleText = EXAMPLE_PASSWORD,
    )
  }

  /**
   * `BCrypt.withDefaults()` uses the strict long password strategy, which rejects passwords longer
   * than 72 bytes instead of silently truncating them (a truncated password would produce a hash
   * that other passwords match too). That error, like an out-of-range cost, gets surfaced by the
   * `errorHolder` of the source input.
   */
  override fun doConvertToTarget(source: ByteArray): ByteArray =
    BCrypt.withDefaults().hash(cost.get(), source)

  override fun Panel.buildSourceTopConfigurationUi() {
    row {
        textField()
          .label(UiToolsBundle.message("bcrypt-transformer.cost"))
          .bindIntTextImproved(cost)
          .validateLongValue(LongRange(BCrypt.MIN_COST.toLong(), BCrypt.MAX_COST.toLong()))
          .comment(UiToolsBundle.message("bcrypt-transformer.cost-comment"))
      }
      .layout(RowLayout.PARENT_GRID)
  }

  // -- Private Methods ----------------------------------------------------- //
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
    private const val EXAMPLE_PASSWORD = "s3cr3t-p4ssw0rd"
  }
}
