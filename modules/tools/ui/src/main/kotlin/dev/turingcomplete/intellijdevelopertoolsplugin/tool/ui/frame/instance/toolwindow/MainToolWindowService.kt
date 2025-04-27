package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.toolwindow

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.content.ContentPanelHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service(Service.Level.PROJECT)
class MainToolWindowService(val project: Project) {
  // -- Properties ---------------------------------------------------------- //

  private val toolWindowLock = ReentrantReadWriteLock()
  private var toolWindow: ToolWindow? = null
  private var deferredToolTask: ToolTask? = null // Also only used under `toolWindowLock`

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun setToolWindow(toolWindow: ToolWindow) {
    toolWindowLock.write {
      this.toolWindow = toolWindow
      if (deferredToolTask != null) {
        when (val toolTask = deferredToolTask!!) {
          is OpenToolTask<*> -> doOpenTool(toolWindow, toolTask.context, toolTask.reference)
          is ShowToolTask -> doShowTool(toolWindow, toolTask.id)
        }
        deferredToolTask = null
      }
    }
  }

  fun <T : OpenDeveloperToolContext> openTool(
    context: T,
    reference: OpenDeveloperToolReference<T>,
  ) {
    toolWindowLock.read {
      if (toolWindow != null) {
        toolWindow!!.show()
        doOpenTool(toolWindow!!, context, reference)
      } else {
        deferredToolTask = OpenToolTask(context, reference)
        // The content of the tool window will be added asynchronous. So, the
        // return of `show()` (or its callback equivalents) will not indicate
        // the `toolWindowContentPanelHandlerKey` was already set. Therefore,
        // we have to defer the open tool task.
        ToolWindowManager.getInstance(project).getToolWindow(MainToolWindowFactory.ID)?.show()
      }
    }
  }

  fun showTool(id: String) {
    toolWindowLock.read {
      if (toolWindow != null) {
        toolWindow!!.show()
        doShowTool(toolWindow!!, id)
      } else {
        deferredToolTask = ShowToolTask(id)
        ToolWindowManager.getInstance(project).getToolWindow(MainToolWindowFactory.ID)?.show()
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun <T : OpenDeveloperToolContext> doOpenTool(
    toolWindow: ToolWindow,
    context: T,
    reference: OpenDeveloperToolReference<out T>,
  ) {
    toolWindow.contentManager
      .getContent(0)
      ?.getUserData(toolWindowContentPanelHandlerKey)
      ?.openTool(context, reference)
  }

  private fun doShowTool(toolWindow: ToolWindow, id: String) {
    toolWindow.contentManager
      .getContent(0)
      ?.getUserData(toolWindowContentPanelHandlerKey)
      ?.showTool(id)
  }

  // -- Inner Type ---------------------------------------------------------- //

  private sealed interface ToolTask

  // -- Inner Type ---------------------------------------------------------- //

  private class OpenToolTask<T : OpenDeveloperToolContext>(
    val context: T,
    val reference: OpenDeveloperToolReference<out T>,
  ) : ToolTask

  // -- Inner Type ---------------------------------------------------------- //

  private class ShowToolTask(val id: String) : ToolTask

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    val toolWindowContentPanelHandlerKey =
      Key<ContentPanelHandler>(ContentPanelHandler::class.simpleName!!)
  }
}
