package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

@Service(Service.Level.APP)
class ExternalSystemProcessRegistry : Disposable {
  // -- Properties ---------------------------------------------------------- //

  private val log = logger<ExternalSystemProcessRegistry>()
  private val lock = Any()
  private val projectsByProcess = LinkedHashMap<ProcessHandler, Project?>()
  private val processesByProject = LinkedHashMap<Project?, LinkedHashSet<ProcessHandler>>()

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun register(project: Project?, process: ProcessHandler) {
    synchronized(lock) {
      projectsByProcess.put(process, project)?.let { previousProject ->
        removeTrackedProcess(process, previousProject)
      }
      processesByProject.getOrPut(project) { linkedSetOf() }.add(process)
    }
  }

  fun unregister(process: ProcessHandler) {
    synchronized(lock) {
      projectsByProcess.remove(process)?.let { project -> removeTrackedProcess(process, project) }
    }
  }

  fun stopProcesses(project: Project) {
    trackedProcesses(project).forEach { stopTrackedProcess(it) }
  }

  override fun dispose() {
    trackedProcesses().forEach { stopTrackedProcess(it) }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun trackedProcesses(project: Project): List<ProcessHandler> =
    synchronized(lock) {
      processesByProject.remove(project)?.toList().orEmpty().onEach { projectsByProcess.remove(it) }
    }

  private fun trackedProcesses(): List<ProcessHandler> =
    synchronized(lock) {
      projectsByProcess.keys.toList().also {
        projectsByProcess.clear()
        processesByProject.clear()
      }
    }

  private fun removeTrackedProcess(process: ProcessHandler, project: Project?) {
    processesByProject[project]?.apply {
      remove(process)
      if (isEmpty()) {
        processesByProject.remove(project)
      }
    }
  }

  private fun stopTrackedProcess(process: ProcessHandler) {
    if (process.isProcessTerminated) {
      return
    }

    runCatching {
        process.destroyProcess()
        if (!process.waitFor(STOP_TIMEOUT_MILLISECONDS)) {
          if (process is KillableProcessHandler && process.canKillProcess()) {
            process.killProcess()
          }
          if (!process.waitFor(STOP_TIMEOUT_MILLISECONDS)) {
            throw IllegalStateException("Timed out while stopping tracked HttpServer process")
          }
        }
      }
      .onFailure { error -> log.warn("Failed to stop tracked HttpServer process", error) }
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val STOP_TIMEOUT_MILLISECONDS = 5_000L
  }
}

@Service(Service.Level.PROJECT)
class HttpServerProjectProcessShutdownService(private val project: Project) : Disposable {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun dispose() {
    ApplicationManager.getApplication()
      .service<ExternalSystemProcessRegistry>()
      .stopProcesses(project)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
