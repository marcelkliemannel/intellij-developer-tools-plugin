package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer

import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory.createScrollPane
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor.EditorMode.OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import javax.swing.JComponent

abstract class TextTransformer(
  private val textTransformerContext: TextTransformerContext,
  protected val context: DeveloperUiToolContext,
  protected val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  protected val project: Project?
) : DeveloperUiTool(parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  protected var liveTransformation = configuration.register("liveTransformation", true)
  protected val sourceText = configuration.register("sourceText", "", PropertyType.INPUT, textTransformerContext.initialSourceExampleText)
  protected var resultText = ValueProperty("")

  private val sourceEditor by lazy { createSourceInputEditor() }
  private val resultEditor by lazy { createResultOutputEditor(parentDisposable) }


  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  protected abstract fun transform()

  override fun Panel.buildUi() {
    buildTopConfigurationUi()

    row {
      cell(sourceEditor.component).align(Align.FILL)
    }.resizableRow()

    buildMiddleConfigurationUi()
    buildActionsUi()

    row {
      cell(resultEditor.component).align(Align.FILL)
    }.resizableRow()
  }

  override fun afterBuildUi() {
    transform()
  }

  protected open fun Panel.buildTopConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildMiddleConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildDebugComponent() {
    throw NotImplementedError("Debug component not implemented")
  }

  protected open fun Row.buildAdditionalActionsUi() {
    // Override if needed
  }

  protected fun setLanguage(language: Language) {
    sourceEditor.language = language
    resultEditor.language = language
  }

  override fun configurationChanged(property: ValueProperty<out Any>) {
    if (!isDisposed && liveTransformation.get()) {
      transform()
    }
  }

  override fun activated() {
    configuration.addChangeListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  override fun doDispose() {
    configuration.removeChangeListener(this)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Panel.buildActionsUi() {
    row {
      val liveTransformationCheckBox = checkBox("Live transformation")
        .bindSelected(liveTransformation)
        .gap(RightGap.SMALL)

      button("▼ ${textTransformerContext.transformActionTitle}") { transform() }
        .enabledIf(liveTransformationCheckBox.selected.not())
        .component

      if (textTransformerContext.supportsDebug) {
        lateinit var debugButton: JComponent
        debugButton = actionButton(
          createDebugAction { debugButton },
          actionPlace = this::class.java.name
        ).component
      }

      buildAdditionalActionsUi()
    }
  }

  private fun createDebugAction(debugButton: () -> JComponent): AnAction {
    return object : DumbAwareAction("Debug", null, AllIcons.Toolwindows.ToolWindowDebugger) {

      override fun actionPerformed(e: AnActionEvent) {
        val debugComponent = panel {
          buildDebugComponent()
        }.apply {
          preferredWidth = 300
        }
        JBPopupFactory.getInstance()
          .createBalloonBuilder(createScrollPane(debugComponent, true))
          .setDialogMode(true)
          .setFillColor(UIUtil.getPanelBackground())
          .setBorderColor(JBColor.border())
          .setBlockClicksThroughBalloon(true)
          .setRequestFocus(true)
          .createBalloon()
          .apply {
            setAnimationEnabled(false)
            show(RelativePoint.getCenterOf(debugButton()), Balloon.Position.atLeft)
          }
      }
    }
  }

  private fun createSourceInputEditor(): DeveloperToolEditor =
    DeveloperToolEditor(
      id = "source-input",
      context = context,
      configuration = configuration,
      project = project,
      title = textTransformerContext.sourceTitle,
      editorMode = INPUT,
      parentDisposable = parentDisposable,
      textProperty = sourceText,
      diffSupport = textTransformerContext.diffSupport?.let { diffSupport ->
        DeveloperToolEditor.DiffSupport(
          title = diffSupport.title,
          secondTitle = textTransformerContext.resultTitle,
          secondText = { resultText.get() },
        )
      },
      initialLanguage = textTransformerContext.inputInitialLanguage ?: PlainTextLanguage.INSTANCE
    ).apply {
      onTextChangeFromUi { _ ->
        if (liveTransformation.get()) {
          transform()
        }
      }
    }

  private fun createResultOutputEditor(parentDisposable: Disposable) =
    DeveloperToolEditor(
      id = "result-output",
      context = context,
      configuration = configuration,
      project = project,
      title = textTransformerContext.resultTitle,
      editorMode = OUTPUT,
      parentDisposable = parentDisposable,
      textProperty = resultText,
      diffSupport = textTransformerContext.diffSupport?.let { diffSupport ->
        DeveloperToolEditor.DiffSupport(
          title = diffSupport.title,
          secondTitle = textTransformerContext.sourceTitle,
          secondText = { sourceText.get() },
        )
      },
      initialLanguage = textTransformerContext.outputInitialLanguage ?: PlainTextLanguage.INSTANCE
    )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class TextTransformerContext(
    val transformActionTitle: String,
    val sourceTitle: String,
    val resultTitle: String,
    val initialSourceText: String? = null,
    val initialSourceExampleText: String? = null,
    val inputInitialLanguage: Language? = null,
    val outputInitialLanguage: Language? = null,
    val diffSupport: DiffSupport? = null,
    val supportsDebug: Boolean = false,
  )

  data class DiffSupport(
    val title: String
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}