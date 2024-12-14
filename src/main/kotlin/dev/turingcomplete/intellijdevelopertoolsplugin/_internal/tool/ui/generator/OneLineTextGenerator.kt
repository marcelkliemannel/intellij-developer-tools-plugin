package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.generator

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.util.Alarm
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.BooleanComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.CopyAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.CopyAction.Companion.CONTENT_DATA_KEY
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor.EditorMode.OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.monospaceFont
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.not
import java.awt.Font

abstract class OneLineTextGenerator(
  private val project: Project?,
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperUiTool(parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  protected val supportsBulkGeneration = BooleanComponentPredicate(true)

  private val generatedText = AtomicProperty("")
  private val invalidConfiguration = AtomicBooleanProperty(false)
  private val generationAlarm by lazy { Alarm(parentDisposable) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  final override fun Panel.buildUi() {
    buildConfigurationUi()
    buildGeneratedValueUi()
    buildAdditionalUi()
    buildBulkGenerationUi()
  }

  protected abstract fun generate(): String

  open fun Panel.buildConfigurationUi() {
    // Override if needed
  }

  open fun Panel.buildAdditionalUi() {
    // Override if needed
  }

  override fun activated() {
    doGenerate()
    configuration.addChangeListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  override fun configurationChanged(property: ValueProperty<out Any>) {
    if (!isDisposed && !configuration.isResetting) {
      doGenerate()
    }
  }

  override fun reset() {
    doGenerate()
  }

  override fun doDispose() {
    configuration.removeChangeListener(this)
  }

  override fun getData(dataId: String): Any? = when {
    CONTENT_DATA_KEY.`is`(dataId) -> generatedText.get()
    else -> super.getData(dataId)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun doGenerate() {
    val generate: () -> Unit = {
      if (validate().isEmpty()) {
        generatedText.set(generate())
        invalidConfiguration.set(false)
      }
      else {
        invalidConfiguration.set(true)
      }
    }
    if (!isDisposed && !generationAlarm.isDisposed) {
      generationAlarm.cancelAllRequests()
      generationAlarm.addRequest(generate, 0)
    }
  }

  private fun Panel.buildBulkGenerationUi() {
    collapsibleGroup("Bulk Generation", false) {
      val resultEditor = DeveloperToolEditor(
        id = "bulk-generation", title = null, editorMode = OUTPUT,
        configuration = configuration, project = project, context = context,
        parentDisposable = parentDisposable
      )

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
        cell(resultEditor.component).align(Align.FILL)
      }.resizableRow()
    }.resizableRow().bottomGap(BottomGap.MEDIUM).visibleIf(supportsBulkGeneration)
  }

  private fun Panel.buildGeneratedValueUi() {
    row {
      label("")
        .bindText(generatedText)
        .monospaceFont(scale = 1.7f, style = Font.BOLD)
        .gap(RightGap.SMALL)
      actionButton(CopyAction()).gap(RightGap.SMALL)
      actionButton(RegenerateAction { doGenerate() })
    }.visibleIf(invalidConfiguration.not())
    row {
      icon(AllIcons.General.BalloonError).gap(RightGap.SMALL)
      label("Invalid configuration").bold()
    }.visibleIf(invalidConfiguration)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class RegenerateAction(private val generateContent: () -> Unit) :
    DumbAwareAction("Regenerate", null, AllIcons.Actions.Refresh) {

    override fun actionPerformed(e: AnActionEvent) {
      generateContent()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_NUMBER_OF_VALUES = "10"

    //val codeFont: Font = JBFont.MONOSPACED EditorColorsManager.getInstance().globalScheme.editorFontName
  }
}

