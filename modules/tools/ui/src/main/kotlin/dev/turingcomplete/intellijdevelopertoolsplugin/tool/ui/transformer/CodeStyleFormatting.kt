package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import com.intellij.codeInsight.actions.RearrangeCodeProcessor
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation

class CodeStyleFormatting(
  private val codeStyles: List<CodeStyle>,
  project: Project,
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextTransformer(
  textTransformerContext = TextTransformerContext(
    transformActionTitle = "Format",
    sourceTitle = "Original",
    resultTitle = "Formatted",
    diffSupport = DiffSupport(
      title = "Code Style Formatting"
    )
  ),
  context = context,
  configuration = configuration,
  parentDisposable = parentDisposable,
  project = project
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedCodeStyleLanguageId = configuration.register("languageId", FAVORITE_DEFAULT_LANGUAGE_ID)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    check(codeStyles.isNotEmpty())

    // Validate if selected language is still available
    if (codeStyles.find { it.language.id == selectedCodeStyleLanguageId.get() } == null) {
      selectedCodeStyleLanguageId.set((codeStyles.find { it.language.id == FAVORITE_DEFAULT_LANGUAGE_ID } ?: codeStyles.first()).language.id)
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildTopConfigurationUi() {
    val selectedCodeStyle = getSelectedCodeStyle()

    row {
      comboBox(codeStyles.toList())
        .label("Language:")
        .applyToComponent {
          selectedItem = selectedCodeStyle
        }
        .whenItemSelectedFromUi {
          selectedCodeStyleLanguageId.set(it.language.id)
          setLanguage(it.language)
        }
    }

    setLanguage(selectedCodeStyle.language)
  }

  override fun transform() {
    val workingVirtualFile = LightVirtualFile(this.javaClass.canonicalName, getSelectedCodeStyle().language, sourceText.get())
    PsiManager.getInstance(project!!).findFile(workingVirtualFile)?.let { workingPsiFile ->
      val processor = RearrangeCodeProcessor(ReformatCodeProcessor(project, workingPsiFile, null, false))
      processor.setPostRunnable {
        resultText.set(workingPsiFile.text)
      }
      processor.run()
    } ?: error("snh: Can't get PSI file for `LightVirtualFile`")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun getSelectedCodeStyle() = codeStyles.first { it.language.id == selectedCodeStyleLanguageId.get() }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class CodeStyle(val title: String, val language: Language) {

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<CodeStyleFormatting> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Code Style Formatting",
      contentTitle = "Code Style Formatting"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> CodeStyleFormatting)? {
      if (project == null) {
        return null
      }

      val codeStyles: List<CodeStyle> = LanguageCodeStyleSettingsProvider
        .EP_NAME.extensionList
        .sortedBy { it.language.displayName }
        .map { CodeStyle(it.language.displayName, it.language) }
      if (codeStyles.isEmpty()) {
        return null
      }

      return { configuration ->
        CodeStyleFormatting(codeStyles, project, context, configuration, parentDisposable)
      }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val FAVORITE_DEFAULT_LANGUAGE_ID = "JSON"
  }
}
