package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.formatter

import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.DynamicDeveloperToolsFactory

class CodeStyleFormattersFactory :
  DynamicDeveloperToolsFactory<CodeStyleFormatter>(
          title = "Code Style Formatting",
          requiresProject = true
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createDeveloperTools(): List<CodeStyleFormatter> =
    LanguageCodeStyleSettingsProvider.EP_NAME.extensionList
            .map { CodeStyleFormatter(it) }
            .sortedBy { it.title }
            .toList()

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}