package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

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
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation

class CodeStyleFormatter(
  private val codeStyles: List<CodeStyle>,
  private val project: Project,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextTransformer(
  presentation = DeveloperToolPresentation("Code Style Formatting", "Code Style Formatter"),
  transformActionTitle = "Format",
  sourceTitle = "Original",
  resultTitle = "Formatted",
  configuration = configuration,
  parentDisposable = parentDisposable
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedCodeStyleLanguageId: String by configuration.register("selectedLanguageId", FAVORITE_DEFAULT_LANGUAGE_ID)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    check(codeStyles.isNotEmpty())

    // Validate if selected language is still available
    if (codeStyles.find { it.language.id == selectedCodeStyleLanguageId } == null) {
      selectedCodeStyleLanguageId = (codeStyles.find { it.language.id == FAVORITE_DEFAULT_LANGUAGE_ID }
                                     ?: codeStyles.first()).language.id
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildConfigurationUi() {
    val selectedCodeStyle = getSelectedCodeStyle()

    row {
      comboBox(codeStyles.toList())
              .label("Language:")
              .applyToComponent {
                selectedItem = selectedCodeStyle
              }
              .whenItemSelectedFromUi {
                selectedCodeStyleLanguageId = it.language.id
                setLanguage(it.language)
              }
    }

    setLanguage(selectedCodeStyle.language)
  }

  override fun transform() {
    val workingVirtualFile = LightVirtualFile(this.javaClass.canonicalName, getSelectedCodeStyle().language, sourceText)
    PsiManager.getInstance(project).findFile(workingVirtualFile)?.let { workingPsiFile ->
      val processor = RearrangeCodeProcessor(ReformatCodeProcessor(project, workingPsiFile, null, false))
      processor.setPostRunnable {
        resultText = workingPsiFile.text
      }
      processor.run()
    } ?: error("snh: Can't get PSI file for `LightVirtualFile`")
  }

  override fun configurationPosition() = ConfigurationPosition.TOP

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun getSelectedCodeStyle() = codeStyles.first { it.language.id == selectedCodeStyleLanguageId }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class CodeStyle(val title: String, val language: Language) {

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool? {
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

      return CodeStyleFormatter(codeStyles, project, configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val FAVORITE_DEFAULT_LANGUAGE_ID = "JSON"
  }
}