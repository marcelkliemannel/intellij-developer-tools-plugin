package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.ui.dsl.builder.Panel
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder

abstract class InputOutputHandler(
  val id: String,
  val title: String,
  val errorHolder: ErrorHolder,
  val liveConversionSupported: Boolean,
  val textDiffSupported: Boolean,
  val inputOutputDirection: InputOutputDirection,
) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  abstract fun Panel.buildUi()

  abstract fun read(): ByteArray

  abstract fun write(output: ByteArray)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
