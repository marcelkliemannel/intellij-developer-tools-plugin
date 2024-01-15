package dev.turingcomplete.intellijdevelopertoolsplugin

import com.intellij.ide.BrowserUtil
import com.intellij.ui.dsl.builder.Row
import org.jetbrains.annotations.Nls

data class DeveloperUiToolPresentation(
  @Nls(capitalization = Nls.Capitalization.Title)
  val menuTitle: String,

  @Nls(capitalization = Nls.Capitalization.Title)
  val contentTitle: String,

  @Nls(capitalization = Nls.Capitalization.Sentence)
  val description: Description? = null
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  interface Description {

    fun Row.buildUi()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ContextHelpDescription(private val description: String) : Description {

    override fun Row.buildUi() {
      contextHelp(description)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ExternalLinkDescription(private val title: String, private val url: String) : Description {

    override fun Row.buildUi() {
      link(title) { BrowserUtil.open(url) }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    fun contextHelp(description: String): Description = ContextHelpDescription(description)

    fun externalLink(title: String, url: String): Description = ExternalLinkDescription(title, url)
  }
}