package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui

import BundleMessagesTest
import com.intellij.testFramework.junit5.RunMethodInEdt
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory

class UiBundleMessagesTest : BundleMessagesTest() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @TestFactory
  override fun `test that all additional languages are containing the same message keys and parameter counts`(): List<DynamicNode> =
    super.`do test that all additional languages are containing the same message keys and parameter counts`()

  @TestFactory
  @RunMethodInEdt(writeIntent = RunMethodInEdt.WriteIntentMode.True)
  override fun `test that all message(messageKey, params) calls are referencing to an existing message key and are using the correct parameters count`(): List<DynamicNode> =
    super.`do test that all message(messageKey, params) calls are referencing to an existing message key and are using the correct parameters count`()

  @TestFactory
  @RunMethodInEdt(writeIntent = RunMethodInEdt.WriteIntentMode.True)
  override fun `test that all keys in the messages bundle are used`(): List<DynamicNode> =
    super.`do test that all keys in the messages bundle are used`()

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}