package dev.turingcomplete.intellijdevelopertoolsplugins.developertool._internal.tool

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import java.time.Duration

abstract class DeveloperToolTestBase<T : DeveloperTool>(
  private val factory: DeveloperToolFactory<T>
) : BasePlatformTestCase() {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  protected lateinit var configuration: DeveloperToolConfiguration
  protected lateinit var developerTool: T

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun setUp() {
    super.setUp()

    configuration = DeveloperToolConfiguration()
    developerTool = checkNotNull(factory.createDeveloperTool(configuration, project, testRootDisposable)).apply {
      runInEdt { createComponent() }
    }
  }

  protected fun runInEdt(runnable: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait { runnable() }
  }

  protected fun shortWait() {
    wait(Duration.ofSeconds(1))
  }

  protected fun wait(time: Duration) {
    if (ApplicationManager.getApplication().isDispatchThread) {
      error("Access from event dispatch thread is not allowed")
    }
    Thread.sleep(time.toMillis())
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}