package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.util.Alarm
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.BooleanComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.CopyAction
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.CopyAction.Companion.CONTENT_DATA_KEY
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ToolBarPlace
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.copyable
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.toMonospace
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.wrapWithToolBar
import org.apache.commons.text.StringEscapeUtils

abstract class OneLineTextGenerator(
  presentation: DeveloperToolContext,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  initialGeneratedTextTitle: String = "Generated text:"
) : DeveloperTool(presentation, parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  protected val supportsBulkGeneration = BooleanComponentPredicate(true)
  protected val generatedTextTitle = AtomicProperty(initialGeneratedTextTitle)

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
    configuration.addChangeListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  override fun configurationChanged() {
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
          text = StringEscapeUtils.escapeHtml4(generate())
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
    private val GENERATED_TEXT_FONT = JBFont.label().toMonospace().biggerOn(1.5f)
    private val GENERATED_TEXT_INVALID_CONFIGURATION_FONT = JBFont.label().biggerOn(1.5f)
  }
}
