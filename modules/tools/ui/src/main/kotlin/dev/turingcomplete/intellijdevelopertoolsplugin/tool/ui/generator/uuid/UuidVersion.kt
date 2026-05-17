package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.generator.uuid

import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle

enum class UuidVersion(val title: String) {
  // -- Values -------------------------------------------------------------- //

  V1(UiToolsBundle.message("uuid-generator.version.v1")),
  V3(UiToolsBundle.message("uuid-generator.version.v3")),
  V4(UiToolsBundle.message("uuid-generator.version.v4")),
  V5(UiToolsBundle.message("uuid-generator.version.v5")),
  V6(UiToolsBundle.message("uuid-generator.version.v6")),
  V7(UiToolsBundle.message("uuid-generator.version.v7"));

  override fun toString(): String = title

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
