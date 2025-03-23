package dev.turingcomplete.intellijdevelopertoolsplugin.javadependent.tool.editor.intention

import IntentionDescriptionTest
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory

class JavaDependentDescriptionTest : IntentionDescriptionTest() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @TestFactory
  override fun `test intention action description HTML file exists`(): List<DynamicNode> =
    super.`do test intention action description HTML file exists`()

  @TestFactory
  override fun `test intention action before template file exists`(): List<DynamicNode> =
    super.`do test intention action before template file exists`()

  @TestFactory
  override fun `test intention action after template file exists`(): List<DynamicNode> =
    super.`do test intention action after template file exists`()

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
