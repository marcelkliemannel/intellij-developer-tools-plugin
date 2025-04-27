package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.menu

import com.intellij.openapi.project.Project

interface ExternalNode {
  // -- Properties ---------------------------------------------------------- //
  // -- Exported Methods ---------------------------------------------------- //

  fun selected(project: Project?)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
