package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

enum class InputOutputDirection(val supportsWrite: Boolean, val supportsRead: Boolean) {
  // -- Values ------------------------------------------------------------- //

  UNDIRECTIONAL_WRITE(supportsWrite = true, supportsRead = false),
  UNDIRECTIONAL_READ(supportsWrite = false, supportsRead = true),
  BIDIRECTIONAL(supportsWrite = true, supportsRead = true),

  // -- Properties --------------------------------------------------------- //
  // -- Initialization ----------------------------------------------------- //
  // -- Exported Methods --------------------------------------------------- //
  // -- Private Methods ---------------------------------------------------- //
  // -- Inner Type --------------------------------------------------------- //
  // -- Companion Object --------------------------------------------------- //
}
