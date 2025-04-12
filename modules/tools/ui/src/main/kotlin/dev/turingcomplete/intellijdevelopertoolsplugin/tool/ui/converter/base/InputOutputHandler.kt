package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import javax.swing.JComponent

abstract class InputOutputHandler(
  val id: String,
  val title: String,
  val errorHolder: ErrorHolder,
  val liveConversionSupported: Boolean,
  val textDiffSupported: Boolean,
) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  abstract fun createComponent(): JComponent

  abstract fun read(): ByteArray

  abstract fun write(output: ByteArray)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
