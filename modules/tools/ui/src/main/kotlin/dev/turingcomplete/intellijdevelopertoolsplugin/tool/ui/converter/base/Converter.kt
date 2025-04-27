package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.ChangeListener
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AsyncTaskExecutor

abstract class Converter(
  private val configuration: DeveloperToolConfiguration,
  private val context: DeveloperUiToolContext,
  protected val project: Project?,
  private val title: String,
  protected val sourceTitle: String,
  private val targetTitle: String,
  private val toTargetTitle: String,
  parentDisposable: Disposable,
) : DeveloperUiTool(parentDisposable), ChangeListener {
  // -- Properties ---------------------------------------------------------- //

  protected var liveConversionEnabled = configuration.register("liveConversion", true)

  protected val conversionEnabled = ValueProperty(true)
  protected val liveConversionSupported = ValueProperty(true)
  private val textDiffSupported = ValueProperty(true)

  protected lateinit var sourceConversionSideHandler: ConversionSideHandler
  protected lateinit var targetConversionSideHandler: ConversionSideHandler

  private val liveConversionExecutor by lazy { AsyncTaskExecutor.onEdt(parentDisposable) }

  protected open val defaultSourceInputOutputHandlerId: String = "source"
  protected open val defaultTargetInputOutputHandlerId: String = "target"

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

  fun convertToTarget() {
    convert(
      heavyConversionTitle = toTargetTitle,
      doConvert = { source, target ->
        // Only check visible/enabled components, because there might be
        // validation errors in non-active `InputOutputHandler`s.
        if (validate(onlyVisibleAndEnabled = true).isNotEmpty()) {
          return@convert
        }

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

  abstract fun requestLiveConversion()

  abstract fun doConvertToTarget(source: ByteArray): ByteArray

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

  abstract fun Panel.buildActionsUi()

  override fun wrapperInsets(): JBInsets =
    super.wrapperInsets().let { JBUI.insets(0, it.left, it.bottom, it.right) }

  override fun afterBuildUi() {
    syncLiveConversionSupported()
    syncTextDiffSupported()
    convertToTarget()
  }

  open fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(id = defaultSourceInputOutputHandlerId)
  }

  protected open fun ConversionSideHandler.addTargetTextInputOutputHandler() {
    addTextInputOutputHandler(id = defaultTargetInputOutputHandlerId)
  }

  protected open fun Panel.buildSourceTopConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildSourceBottomConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildTargetTopConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildBottomConfigurationUi() {
    // Override if needed
  }

  protected open fun Row.buildAdditionalActionsUi() {
    // Override if needed
  }

  protected fun isLiveConversionAvailable(): Boolean =
    liveConversionEnabled.get() && liveConversionSupported.get()

  // -- Private Methods ----------------------------------------------------- //

  private fun ConversionSideHandler.initSourceConversionSide() {
    addSourceTextInputOutputHandler()
    addFileInputOutputHandler(defaultSourceInputOutputHandlerId)
  }

  private fun ConversionSideHandler.initTargetConversionSide() {
    addTargetTextInputOutputHandler()
    addFileInputOutputHandler(defaultTargetInputOutputHandlerId)
  }

  private fun Panel.buildSourceUi() {
    sourceConversionSideHandler =
      createSourceConversionSideHandler(
        createDiffSupport(
          secondTitle = targetTitle,
          secondText = { String(targetConversionSideHandler.activeInputOutputHandler.get().read()) },
        )
      )
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

    buildSourceBottomConfigurationUi()
  }

  private fun Panel.buildTargetUi() {
    targetConversionSideHandler =
      createTargetConversionSideHandler(
        diffSupport =
          createDiffSupport(
            secondTitle = sourceTitle,
            secondText = {
              String(sourceConversionSideHandler.activeInputOutputHandler.get().read())
            },
          )
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

  protected abstract fun createSourceConversionSideHandler(
    diffSupport: AdvancedEditor.DiffSupport
  ): ConversionSideHandler

  protected abstract fun createTargetConversionSideHandler(
    diffSupport: AdvancedEditor.DiffSupport
  ): ConversionSideHandler

  protected open fun activeInputOutputHandlerChanged(
    conversionSideHandler: ConversionSideHandler,
    inputOutputHandler: InputOutputHandler,
  ) {
    // Override if needed
  }

  private fun createActiveInputOutputHandlerListener(
    parentConversionSideHandler: ConversionSideHandler
  ): (InputOutputHandler) -> Unit = {
    activeInputOutputHandlerChanged(parentConversionSideHandler, it)
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

  protected fun createConversionSideHandler(
    title: String,
    diffSupport: AdvancedEditor.DiffSupport,
    liveConversionRequested: () -> Unit,
    inputOutputDirection: InputOutputDirection,
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
      inputOutputDirection = inputOutputDirection,
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

  protected fun convert(
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
