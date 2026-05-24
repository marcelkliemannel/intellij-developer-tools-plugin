package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import com.intellij.codeInsight.actions.RearrangeCodeProcessor
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
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
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

class CodeStyleFormatting(
  val codeStyles: List<CodeStyle>,
  project: Project,
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
) :
  TextTransformer(
    textTransformerContext =
      TextTransformerContext(
        transformActionTitle = UiToolsBundle.message("code-style-formatting.format"),
        sourceTitle = UiToolsBundle.message("code-style-formatting.source-title"),
        resultTitle = UiToolsBundle.message("code-style-formatting.result-title"),
        diffSupport = DiffSupport(title = UiToolsBundle.message("code-style-formatting.title")),
      ),
    context = context,
    configuration = configuration,
    parentDisposable = parentDisposable,
    project = project,
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var selectedCodeStyleLanguageId =
    configuration.register("languageId", FAVORITE_DEFAULT_LANGUAGE_ID)

  // -- Initialization ------------------------------------------------------ //

  init {
    check(codeStyles.isNotEmpty())

    // Validate if selected language is still available
    if (codeStyles.find { it.language.id == selectedCodeStyleLanguageId.get() } == null) {
      selectedCodeStyleLanguageId.set(
        (codeStyles.find { it.language.id == FAVORITE_DEFAULT_LANGUAGE_ID } ?: codeStyles.first())
          .language
          .id
      )
    }
  }

  // -- Exposed Methods ----------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildTopConfigurationUi() {
    val selectedCodeStyle = getSelectedCodeStyle()

    row {
      comboBox(codeStyles.toList())
        .label(UiToolsBundle.message("code-style-formatting.language"))
        .applyToComponent { selectedItem = selectedCodeStyle }
        .whenItemSelectedFromUi {
          selectedCodeStyleLanguageId.set(it.language.id)
          setLanguage(it.language)
        }
    }

    setLanguage(selectedCodeStyle.language)
  }

  override fun transform() {
    val workingVirtualFile =
      LightVirtualFile(
        this.javaClass.canonicalName,
        getSelectedCodeStyle().language,
        sourceText.get(),
      )

    val workingPsiFile =
      ReadAction.computeBlocking<com.intellij.psi.PsiFile?, RuntimeException> {
        PsiManager.getInstance(project!!).findFile(workingVirtualFile)
      }

    if (workingPsiFile != null) {
      val processor =
        RearrangeCodeProcessor(ReformatCodeProcessor(project, workingPsiFile, null, false))
      processor.setPostRunnable {
        val text = ReadAction.computeBlocking<String, RuntimeException> { workingPsiFile.text }
        resultText.set(text)
      }
      processor.run()
    } else {
      error("snh: Can't get PSI file for `LightVirtualFile`")
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun getSelectedCodeStyle() =
    codeStyles.first { it.language.id == selectedCodeStyleLanguageId.get() }

  // -- Inner Type ---------------------------------------------------------- //

  data class CodeStyle(val title: String, val language: Language) {

    override fun toString(): String = title
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<CodeStyleFormatting> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("code-style-formatting.menu-title"),
        contentTitle = UiToolsBundle.message("code-style-formatting.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> CodeStyleFormatting)? {
      if (project == null) {
        return null
      }

      val codeStyles: List<CodeStyle> =
        LanguageCodeStyleSettingsProvider.EP_NAME.extensionList
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

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val FAVORITE_DEFAULT_LANGUAGE_ID = "JSON"
  }
}
