package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.formatter

import com.intellij.codeInsight.actions.RearrangeCodeProcessor
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.layout.not
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.BooleanPropertyComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DeveloperToolEditor.EditorMode.OUTPUT

class CodeStyleFormatter(private val codeStyle: LanguageCodeStyleSettingsProvider) :
  DeveloperTool(
          id = "${codeStyle.language.id}-code-formatter",
          title = codeStyle.language.displayName
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var originalEditor: DeveloperToolEditor
  private lateinit var formattedEditor: DeveloperToolEditor
  private val formattingInProgress = BooleanPropertyComponentPredicate(false)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi(project: Project?, parentDisposable: Disposable) {
    row {
      resizableRow()
      originalEditor = DeveloperToolEditor(id = id, title = "Original", editorMode = INPUT, language = codeStyle.language)
      cell(originalEditor.createComponent(parentDisposable)).align(Align.FILL)
    }

    row {
      button("▼ Format") { doFormat(project!!) }.enabledIf(formattingInProgress.not())
    }

    row {
      resizableRow()
      formattedEditor = DeveloperToolEditor(id = id, title = "Formatted", editorMode = OUTPUT, language = codeStyle.language)
      cell(formattedEditor.createComponent(parentDisposable)).align(Align.FILL)
    }
  }

  private fun doFormat(project: Project) {
    if (formattingInProgress.value) {
      return
    }

    formattingInProgress.value = true

    val originalText = originalEditor.text
    val workingVirtualFile = LightVirtualFile(id, codeStyle.language, originalText)
    PsiManager.getInstance(project).findFile(workingVirtualFile)?.let { workingPsiFile ->
      val processor = RearrangeCodeProcessor(ReformatCodeProcessor(project, workingPsiFile, null, false))
      processor.setPostRunnable {
        if (!formattedEditor.isDisposed) {
          formattedEditor.text = workingPsiFile.text
          formattingInProgress.value = false
        }
      }
      processor.run()
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}