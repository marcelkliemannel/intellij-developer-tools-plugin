package dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.tool.editor.intention

import DescriptionTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class KotlinDependentDescriptionTest : DescriptionTest() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @ParameterizedTest
  @MethodSource("intentionActionSimpleClassNames")
  override fun testIntentionActionDescriptionHtmlFileExist(intentionActionSimpleClassName: String) {
    super.doTestIntentionActionDescriptionHtmlFileExist(intentionActionSimpleClassName)
  }

  @ParameterizedTest
  @MethodSource("intentionActionSimpleClassNames")
  override fun testIntentionActionBeforeTemplateFileExist(intentionActionSimpleClassName: String) {
    super.doTestIntentionActionBeforeTemplateFileExist(intentionActionSimpleClassName)
  }

  @ParameterizedTest
  @MethodSource("intentionActionSimpleClassNames")
  override fun testIntentionActionAfterTemplateFileExist(intentionActionSimpleClassName: String) {
    super.doTestIntentionActionAfterTemplateFileExist(intentionActionSimpleClassName)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    @JvmStatic
    fun intentionActionSimpleClassNames() =
      intentionActionSimpleClassNames("dev.turingcomplete.intellijdevelopertoolsplugin.kotlindependent.tool.editor.intention")
  }
}