package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugins.ToolBarPlace
import dev.turingcomplete.intellijdevelopertoolsplugins.UiUtils.MONOSPACE_FONT
import dev.turingcomplete.intellijdevelopertoolsplugins.copyable
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.CopyAction
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.wrapWithToolBar
import kotlinx.serialization.json.Json.Default.configuration

abstract class OneLineTextGenerator(
        id: String,
        title: String,
        private val supportsBulkGeneration: Boolean = true,
        description: String? = null
) : DeveloperTool(id = id, title = title, description = description) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var generatedTextLabel: JBLabel

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    registerPropertyChangeListeners { doGenerate() }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  final override fun Panel.buildUi(project: Project?, parentDisposable: Disposable) {
    buildConfigurationUi(project, parentDisposable)
    buildGeneratedValueUi()
    if (supportsBulkGeneration) {
      buildBulkGenerationUi(parentDisposable)
    }
  }

  protected abstract fun generate(): String

  open fun Panel.buildConfigurationUi(project: Project?, parentDisposable: Disposable) {}

  override fun activated() {
    doGenerate()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun doGenerate() {
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

  private fun Panel.buildBulkGenerationUi(parentDisposable: Disposable) {
    group("Bulk Generation", false) {
      val resultEditor = DeveloperToolEditor(id = id, title = null, editorMode = OUTPUT)

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
        resizableRow()
        cell(resultEditor.createComponent(parentDisposable)).align(Align.FILL)
      }
    }.resizableRow().bottomGap(BottomGap.MEDIUM)
  }

  private fun Panel.buildGeneratedValueUi() {
    row {
      label("Generated $title:")
      bottomGap(BottomGap.NONE)
    }

    row {
      topGap(TopGap.NONE)
      generatedTextLabel = JBLabel().apply { font = GENERATED_TEXT_FONT }.copyable()

      val actions = DefaultActionGroup().apply {
        add(RefreshAction { doGenerate() })
        add(CopyAction { generatedTextLabel.text })
      }
      cell(generatedTextLabel.wrapWithToolBar("$id-generated-text", actions, ToolBarPlace.APPEND, false))
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

    private val GENERATED_TEXT_FONT = MONOSPACE_FONT.biggerOn(2f)
    private val GENERATED_TEXT_INVALID_CONFIGURATION_FONT = JBFont.label().biggerOn(2f)
  }
}

