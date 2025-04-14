package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.util.or
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.layout.and
import com.intellij.ui.layout.not
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.not
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.ChangeListener
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AsyncTaskExecutor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate.Companion.createPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

abstract class Converter(
  private val configuration: DeveloperToolConfiguration,
  private val context: DeveloperUiToolContext,
  private val project: Project?,
  private val title: String,
  private val sourceTitle: String,
  private val targetTitle: String,
  private val toTargetTitle: String,
  private val toSourceTitle: String,
  parentDisposable: Disposable,
) : DeveloperUiTool(parentDisposable), ChangeListener {
  // -- Properties ---------------------------------------------------------- //

  private var liveConversionEnabled = configuration.register("liveConversion", true)

  private val conversionEnabled = ValueProperty<Boolean>(true)
  private val liveConversionSupported = ValueProperty<Boolean>(true)
  private val textDiffSupported = ValueProperty<Boolean>(true)

  protected lateinit var sourceConversionSideHandler: ConversionSideHandler
  protected lateinit var targetConversionSideHandler: ConversionSideHandler
  private lateinit var activeConversionSideHandler: ValueProperty<ConversionSideHandler>

  private val liveConversionExecutor by lazy { AsyncTaskExecutor.onEdt(parentDisposable) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun configurationChanged(property: ValueProperty<out Any>) {
    requestLiveConversion()
  }

  override fun activated() {
    configuration.addChangeListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

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

  fun convertToTarget() {
    convert(
      heavyConversionTitle = toTargetTitle,
      doConvert = { source, target ->
        source.errorHolder.catchException {
          val result = doConvertToTarget(source.read())
          target.errorHolder.catchException { target.write(result) }
        }
      },
    )
  }

  fun requestLiveConversionToTarget() {
    if (isLiveConversionAvailable()) {
      convertToTarget()
    }
  }

  fun requestLiveConversionToSource() {
    if (isLiveConversionAvailable()) {
      convertToSource()
    }
  }

  fun requestLiveConversion() {
    if (isLiveConversionAvailable()) {
      if (activeConversionSideHandler.get() == sourceConversionSideHandler) {
        convertToTarget()
      } else {
        convertToSource()
      }
    }
  }

  abstract fun doConvertToTarget(source: ByteArray): ByteArray

  abstract fun doConvertToSource(target: ByteArray): ByteArray

  override fun Panel.buildUi() {
    row {
        cell(
            Splitter(true, 0.48f).apply {
              firstComponent = com.intellij.ui.dsl.builder.panel { buildSourceUi() }
              secondComponent =
                com.intellij.ui.dsl.builder.panel {
                  buildActionsUi()
                  buildTargetUi()
                }
            }
          )
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()

    buildBottomConfigurationUi()
  }

  override fun wrapperInsets(): JBInsets =
    super.wrapperInsets().let { JBUI.insets(0, it.left, it.bottom, it.right) }

  override fun afterBuildUi() {
    syncLiveConversionSupported()
    syncTextDiffSupported()
    convertToTarget()
  }

  open fun ConversionSideHandler.initSourceConversionSide() {
    addTextInputOutputHandler("source")
    addFileInputOutputHandler("source")
  }

  open fun ConversionSideHandler.initTargetConversionSide() {
    addTextInputOutputHandler("target")
    addFileInputOutputHandler("target")
  }

  open fun Panel.buildSourceTopConfigurationUi() {
    // Override if needed
  }

  open fun Panel.buildTargetTopConfigurationUi() {
    // Override if needed
  }

  open fun Panel.buildBottomConfigurationUi() {
    // Override if needed
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun isLiveConversionAvailable(): Boolean =
    liveConversionEnabled.get() && liveConversionSupported.get()

  private fun Panel.buildSourceUi() {
    sourceConversionSideHandler =
      createConversionSideHandler(
        title = sourceTitle,
        diffSupport =
          createDiffSupport(
            secondTitle = targetTitle,
            secondText = {
              String(targetConversionSideHandler.activeInputOutputHandler.get().read())
            },
          ),
        liveConversionRequested = { requestLiveConversionToTarget() },
      )
    activeConversionSideHandler = ValueProperty(sourceConversionSideHandler)
    with(sourceConversionSideHandler) {
      initSourceConversionSide()

      buildSourceTopConfigurationUi()

      row { cell(createComponent()).align(Align.FILL) }
        .resizableRow()
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      activeInputOutputHandler.afterChange(
        parentDisposable,
        createActiveInputOutputHandlerListener(sourceConversionSideHandler),
      )
    }
  }

  private fun Panel.buildTargetUi() {
    targetConversionSideHandler =
      createConversionSideHandler(
        title = targetTitle,
        diffSupport =
          createDiffSupport(
            secondTitle = sourceTitle,
            secondText = {
              String(sourceConversionSideHandler.activeInputOutputHandler.get().read())
            },
          ),
        liveConversionRequested = { requestLiveConversionToSource() },
      )
    with(targetConversionSideHandler) {
      initTargetConversionSide()

      buildTargetTopConfigurationUi()

      row { cell(createComponent()).align(Align.FILL) }
        .resizableRow()
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      activeInputOutputHandler.afterChange(
        parentDisposable,
        createActiveInputOutputHandlerListener(targetConversionSideHandler),
      )
    }
  }

  private fun createActiveInputOutputHandlerListener(
    parentConversionSideHandler: ConversionSideHandler
  ): (InputOutputHandler) -> Unit = {
    activeConversionSideHandler.set(parentConversionSideHandler)
    syncLiveConversionSupported()
    syncTextDiffSupported()
    requestLiveConversion()
  }

  private fun createDiffSupport(secondTitle: String, secondText: () -> String) =
    AdvancedEditor.DiffSupport(
      title = title,
      secondTitle = secondTitle,
      enabled = textDiffSupported,
      secondText = secondText,
    )

  private fun doValidate() {
    ApplicationManager.getApplication().invokeLater {
      // The `validate` in this class is not used as a validation mechanism. We
      // make use of its text field error UI to display the `errorHolder`.
      validate()
    }
  }

  private fun createConversionSideHandler(
    title: String,
    diffSupport: AdvancedEditor.DiffSupport,
    liveConversionRequested: () -> Unit,
  ) =
    ConversionSideHandler(
      title = title,
      configuration = configuration,
      project = project,
      context = context,
      parentDisposable = parentDisposable,
      conversionEnabled = conversionEnabled,
      liveConversionRequested = liveConversionRequested,
      diffSupport = diffSupport,
    )

  private fun syncLiveConversionSupported() {
    liveConversionSupported.set(
      sourceConversionSideHandler.activeInputOutputHandler.get().liveConversionSupported &&
        targetConversionSideHandler.activeInputOutputHandler.get().liveConversionSupported
    )
  }

  private fun syncTextDiffSupported() {
    textDiffSupported.set(
      sourceConversionSideHandler.activeInputOutputHandler.get().textDiffSupported &&
        targetConversionSideHandler.activeInputOutputHandler.get().textDiffSupported
    )
  }

  private fun Panel.buildActionsUi() {
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
        }
        .enabledIf(conversionEnabled.createPredicate(true))
        .bottomGap(BottomGap.NONE)
        .topGap(TopGap.NONE)
    }
  }

  private fun convert(
    heavyConversionTitle: String,
    doConvert: (InputOutputHandler, InputOutputHandler) -> Unit,
  ) {
    val sourceInputOutputHandler: InputOutputHandler =
      sourceConversionSideHandler.activeInputOutputHandler.get()
    val targetInputOutputHandler: InputOutputHandler =
      targetConversionSideHandler.activeInputOutputHandler.get()

    sourceInputOutputHandler.errorHolder.clear()
    targetInputOutputHandler.errorHolder.clear()

    if (
      sourceInputOutputHandler.liveConversionSupported &&
        targetInputOutputHandler.liveConversionSupported
    ) {
      if (!liveConversionExecutor.isDisposed) {
        liveConversionExecutor.cancelAll()
        liveConversionExecutor.enqueueTask(
          {
            doConvert(sourceInputOutputHandler, targetInputOutputHandler)
            doValidate()
          },
          100,
        )
      }
      return
    }

    conversionEnabled.set(false)
    object : Task.Backgroundable(project, heavyConversionTitle) {

        override fun run(indicator: ProgressIndicator) {
          indicator.text = heavyConversionTitle
          doConvert(sourceInputOutputHandler, targetInputOutputHandler)
        }

        override fun onFinished() {
          conversionEnabled.set(true)
          doValidate()
        }
      }
      .queue()
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
