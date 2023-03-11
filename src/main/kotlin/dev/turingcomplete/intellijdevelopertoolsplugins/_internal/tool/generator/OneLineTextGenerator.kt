package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.util.Alarm
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.*
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.CopyAction.Companion.CONTENT_DATA_KEY
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.OUTPUT

abstract class OneLineTextGenerator(
  presentation: DeveloperToolPresentation,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperTool(presentation, parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  protected val supportsBulkGeneration = BooleanComponentPredicate(true)
  protected val generatedTextTitle = AtomicProperty("Generated text:")

  private lateinit var generatedTextLabel: JBLabel
  private val generationAlarm by lazy { Alarm(parentDisposable) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  final override fun Panel.buildUi() {
    buildConfigurationUi()
    buildGeneratedValueUi()
    buildBulkGenerationUi()
  }

  protected abstract fun generate(): String

  open fun Panel.buildConfigurationUi() {
    // Override if needed
  }

  override fun activated() {
    doGenerate()
    configuration.addChangeListener(this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  override fun configurationChanged(key: String) {
    if (!isDisposed) {
      doGenerate()
    }
  }

  override fun doDispose() {
    configuration.removeChangeListener(this)
  }

  override fun getData(dataId: String): Any? = when {
    CONTENT_DATA_KEY.`is`(dataId) -> generatedTextLabel.text
    else -> super.getData(dataId)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun doGenerate() {
    generationAlarm.cancelAllRequests()
    val generate: () -> Unit = {
      if (validate().isEmpty()) {
        generatedTextLabel.apply {
          text = generate()
          icon = null
          font = GENERATED_TEXT_FONT
        }
      }
      else {
        generatedTextLabel.apply {
          text = "Invalid configuration"
          icon = AllIcons.General.BalloonError
          font = GENERATED_TEXT_INVALID_CONFIGURATION_FONT
        }
      }
    }
    generationAlarm.addRequest(generate, 0)
  }

  private fun Panel.buildBulkGenerationUi() {
    collapsibleGroup("Bulk Generation", false) {
      val resultEditor = DeveloperToolEditor(title = null, editorMode = OUTPUT, parentDisposable = parentDisposable)

      row {
        label("Number of values:").gap(RightGap.SMALL)

        val numberOfValuesTextField = intTextField(IntRange(1, 99999)).columns(COLUMNS_TINY).applyToComponent {
          text = DEFAULT_NUMBER_OF_VALUES
        }.gap(RightGap.SMALL)

        button("Generate") {
          configuration
          if (validate().isEmpty()) {
            resultEditor.text = IntRange(1, numberOfValuesTextField.component.text.toInt())
                    .joinToString(System.lineSeparator()) { generate() }
          }
        }
      }

      row {
        cell(resultEditor.createComponent()).align(Align.FILL)
      }.resizableRow()
    }.resizableRow().bottomGap(BottomGap.MEDIUM).visibleIf(supportsBulkGeneration)
  }

  private fun Panel.buildGeneratedValueUi() {
    row {
      label("").bindText(generatedTextTitle)
    }.bottomGap(BottomGap.NONE)

    row {
      topGap(TopGap.NONE)
      generatedTextLabel = JBLabel().apply { font = GENERATED_TEXT_FONT }.copyable()

      val actions = DefaultActionGroup().apply {
        add(RefreshAction { doGenerate() })
        add(CopyAction())
      }
      cell(generatedTextLabel.wrapWithToolBar(this.javaClass.simpleName, actions, ToolBarPlace.APPEND))
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class RefreshAction(private val generateContent: () -> Unit) : DumbAwareAction(AllIcons.Actions.Refresh) {

    override fun actionPerformed(e: AnActionEvent) {
      generateContent()
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_NUMBER_OF_VALUES = "10"
    private val GENERATED_TEXT_FONT = JBFont.label().toMonospace().biggerOn(2f)
    private val GENERATED_TEXT_INVALID_CONFIGURATION_FONT = JBFont.label().biggerOn(2f)
  }
}

