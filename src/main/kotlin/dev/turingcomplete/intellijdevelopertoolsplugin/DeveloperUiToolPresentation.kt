package dev.turingcomplete.intellijdevelopertoolsplugin

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

data class DeveloperUiToolPresentation(
  @Nls(capitalization = Nls.Capitalization.Title)
  val menuTitle: String,

  @Nls(capitalization = Nls.Capitalization.Title)
  val groupedMenuTitle: String = menuTitle,

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

    fun show(parentComponent: JComponent)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ContextHelpDescription(private val description: String) : Description {

    override fun show(parentComponent: JComponent) {
      val panel = panel {
        row {
          label("<html>$description</html>")
        }
      }
      JBPopupFactory.getInstance().createBalloonBuilder(panel)
        .setDialogMode(true)
        .setFillColor(UIUtil.getPanelBackground())
        .setBorderColor(JBColor.border())
        .setBlockClicksThroughBalloon(true)
        .setRequestFocus(true)
        .createBalloon()
        .apply {
          setAnimationEnabled(false)
          show(RelativePoint.getSouthOf(parentComponent), Balloon.Position.below)
        }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ExternalLinkDescription(private val url: String) : Description {

    override fun show(parentComponent: JComponent) {
      BrowserUtil.open(url)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    fun contextHelp(description: String): Description = ContextHelpDescription(description)

    fun externalLink(url: String): Description = ExternalLinkDescription(url)
  }
}