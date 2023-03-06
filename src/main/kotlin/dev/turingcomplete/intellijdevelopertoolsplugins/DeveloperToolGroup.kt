package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.annotations.Nls

class DeveloperToolGroup {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  @Attribute("id")
  @RequiredElement
  lateinit var id: String

  @Attribute("menuTitle")
  @RequiredElement
  @Nls(capitalization = Nls.Capitalization.Title)
  lateinit var menuTitle: String

  @Attribute("detailTitle")
  @RequiredElement
  @Nls(capitalization = Nls.Capitalization.Title)
  lateinit var detailTitle: String

  @Attribute("initiallyExpanded")
  var initiallyExpanded: Boolean? = false

  @Attribute("weight")
  var weight: Int? = 1

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val EP_NAME: ExtensionPointName<DeveloperToolGroup> =
      ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.developerToolGroup")
  }
}