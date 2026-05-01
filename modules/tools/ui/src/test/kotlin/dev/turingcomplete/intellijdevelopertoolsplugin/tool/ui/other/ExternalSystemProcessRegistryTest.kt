package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.execution.process.ProcessHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures.IdeaTest
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExternalSystemProcessRegistryTest : IdeaTest() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  @Test
  fun `stops project processes when a project is disposed`() {
    val registry = ExternalSystemProcessRegistry()
    val projectProcess = TestProcess()
    val applicationProcess = TestProcess()

    registry.register(fixture.project, projectProcess)
    registry.register(null, applicationProcess)

    registry.stopProcesses(fixture.project)

    assertThat(projectProcess.destroyProcessCalls).isEqualTo(1)
    assertThat(projectProcess.isProcessTerminated).isTrue()
    assertThat(applicationProcess.destroyProcessCalls).isZero()
    assertThat(applicationProcess.isProcessTerminated).isFalse()
  }

  @Test
  fun `does not stop unregistered processes on shutdown`() {
    val registry = ExternalSystemProcessRegistry()
    val process = TestProcess()

    registry.register(fixture.project, process)
    registry.unregister(process)
    registry.dispose()

    assertThat(process.destroyProcessCalls).isZero()
    assertThat(process.isProcessTerminated).isFalse()
  }

  @Test
  fun `stops remaining processes on application shutdown`() {
    val registry = ExternalSystemProcessRegistry()
    val process = TestProcess()

    registry.register(null, process)
    registry.dispose()

    assertThat(process.destroyProcessCalls).isEqualTo(1)
    assertThat(process.isProcessTerminated).isTrue()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class TestProcess : ProcessHandler() {

    var destroyProcessCalls = 0
      private set

    init {
      startNotify()
    }

    override fun destroyProcessImpl() {
      destroyProcessCalls++
      notifyProcessTerminated(0)
    }

    override fun detachProcessImpl() {
      notifyProcessDetached()
    }

    override fun detachIsDefault(): Boolean = false

    override fun getProcessInput(): OutputStream = ByteArrayOutputStream()
  }

  // -- Companion Object ---------------------------------------------------- //
}
