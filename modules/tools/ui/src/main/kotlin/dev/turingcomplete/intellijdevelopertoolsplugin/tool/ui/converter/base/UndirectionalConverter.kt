package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.util.or
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import dev.turingcomplete.intellijdevelopertoolsplugin.common.not
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate.Companion.createPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

abstract class UndirectionalConverter(
  configuration: DeveloperToolConfiguration,
  context: DeveloperUiToolContext,
  project: Project?,
  title: String,
  sourceTitle: String,
  private val targetTitle: String,
  private val toTargetTitle: String,
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
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun requestLiveConversion() {
    if (isLiveConversionAvailable()) {
      convertToTarget()
    }
  }

  override fun Panel.buildActionsUi() {
    buttonsGroup {
      row {
          val liveConversionSupportedPredicate = liveConversionSupported.createPredicate(true)

          checkBox(UiToolsBundle.message("converter.live-conversion"))
            .bindSelected(liveConversionEnabled)
            .visibleIf(liveConversionSupportedPredicate)

          button("▼ $toTargetTitle") { convertToTarget() }
            .enabledIf(liveConversionEnabled.not().or(liveConversionSupported.not()))
            .gap(RightGap.SMALL)

          buildAdditionalActionsUi()
        }
        .enabledIf(conversionEnabled.createPredicate(true))
        .bottomGap(BottomGap.NONE)
        .topGap(TopGap.NONE)
    }
  }

  final override fun createSourceConversionSideHandler(diffSupport: AdvancedEditor.DiffSupport) =
    createConversionSideHandler(
      title = sourceTitle,
      diffSupport = diffSupport,
      liveConversionRequested = { requestLiveConversionToTarget() },
      inputOutputDirection = InputOutputDirection.UNDIRECTIONAL_READ,
    )

  final override fun createTargetConversionSideHandler(diffSupport: AdvancedEditor.DiffSupport) =
    createConversionSideHandler(
      title = targetTitle,
      diffSupport = diffSupport,
      liveConversionRequested = { /* nothing to do */ },
      inputOutputDirection = InputOutputDirection.UNDIRECTIONAL_WRITE,
    )

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
