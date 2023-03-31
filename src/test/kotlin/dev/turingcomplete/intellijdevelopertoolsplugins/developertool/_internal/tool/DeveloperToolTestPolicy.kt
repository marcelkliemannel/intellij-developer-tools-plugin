package dev.turingcomplete.intellijdevelopertoolsplugins.developertool._internal.tool

import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy

@Suppress("unused") // Referenced in build.gradle.kts
class DeveloperToolTestPolicy  : IdeaTestExecutionPolicy() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun getName(): String = "developer-tool-test"

  override fun runInDispatchThread(): Boolean = false

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}