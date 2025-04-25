package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.util.or
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.layout.and
import com.intellij.ui.layout.not
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.not
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate.Companion.createPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

abstract class BidirectionalConverter(
  configuration: DeveloperToolConfiguration,
  context: DeveloperUiToolContext,
  project: Project?,
  title: String,
  sourceTitle: String,
  private val targetTitle: String,
  private val toTargetTitle: String,
  private val toSourceTitle: String,
  parentDisposable: Disposable,
) :
  Converter(
    configuration = configuration,
    context = context,
    project = project,
    title = title,
    sourceTitle = sourceTitle,
    targetTitle = targetTitle,
    toTargetTitle = toTargetTitle,
    parentDisposable = parentDisposable,
  ) {
  // -- Properties ---------------------------------------------------------- //

  private lateinit var activeConversionSideHandler: ValueProperty<ConversionSideHandler>

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun requestLiveConversion() {
    if (isLiveConversionAvailable()) {
      if (activeConversionSideHandler.get() == sourceConversionSideHandler) {
        convertToTarget()
      } else {
        convertToSource()
      }
    }
  }

  override fun Panel.buildActionsUi() {
    activeConversionSideHandler = ValueProperty(sourceConversionSideHandler)

    buttonsGroup {
      row {
          val liveConversionSupportedPredicate = liveConversionSupported.createPredicate(true)
          val sourceConversionSideActivePredicate =
            activeConversionSideHandler.createPredicate(sourceConversionSideHandler)

          checkBox(UiToolsBundle.message("converter.live-conversion"))
            .bindSelected(liveConversionEnabled)
            .gap(RightGap.SMALL)
            .visibleIf(liveConversionSupportedPredicate)

          icon(AllIcons.General.ArrowUp)
            .visibleIf(
              sourceConversionSideActivePredicate.not().and(liveConversionSupportedPredicate)
            )
            .gap(RightGap.SMALL)

          icon(AllIcons.General.ArrowDown)
            .visibleIf(sourceConversionSideActivePredicate.and(liveConversionSupportedPredicate))
            .gap(RightGap.SMALL)

          button("▼ $toTargetTitle") { convertToTarget() }
            .enabledIf(liveConversionEnabled.not().or(liveConversionSupported.not()))
            .gap(RightGap.SMALL)
          button("▲ $toSourceTitle") { convertToSource() }
            .enabledIf(liveConversionEnabled.not().or(liveConversionSupported.not()))

          buildAdditionalActionsUi()
        }
        .enabledIf(conversionEnabled.createPredicate(true))
        .bottomGap(BottomGap.NONE)
        .topGap(TopGap.NONE)
    }
  }

  override fun activeInputOutputHandlerChanged(
    conversionSideHandler: ConversionSideHandler,
    inputOutputHandler: InputOutputHandler,
  ) {
    activeConversionSideHandler.set(conversionSideHandler)
  }

  final override fun createSourceConversionSideHandler(diffSupport: AdvancedEditor.DiffSupport) =
    createConversionSideHandler(
      title = sourceTitle,
      diffSupport = diffSupport,
      liveConversionRequested = { requestLiveConversionToTarget() },
      inputOutputDirection = InputOutputDirection.BIDIRECTIONAL,
    )

  final override fun createTargetConversionSideHandler(diffSupport: AdvancedEditor.DiffSupport) =
    createConversionSideHandler(
      title = targetTitle,
      diffSupport = diffSupport,
      liveConversionRequested = { requestLiveConversionToSource() },
      inputOutputDirection = InputOutputDirection.BIDIRECTIONAL,
    )

  fun convertToSource() {
    convert(
      heavyConversionTitle = toSourceTitle,
      doConvert = { source, target ->
        target.errorHolder.catchException {
          val result = doConvertToSource(target.read())
          source.errorHolder.catchException { source.write(result) }
        }
      },
    )
  }

  abstract fun doConvertToSource(target: ByteArray): ByteArray

  fun requestLiveConversionToSource() {
    if (isLiveConversionAvailable()) {
      convertToSource()
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
