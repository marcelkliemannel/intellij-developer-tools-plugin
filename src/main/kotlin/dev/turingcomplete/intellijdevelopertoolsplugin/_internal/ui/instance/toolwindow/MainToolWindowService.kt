package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.toolwindow

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.content.ContentPanelHandler
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.handling.OpenDeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.handling.OpenDeveloperToolReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service(Service.Level.PROJECT)
internal class MainToolWindowService(val project: Project) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val toolWindowLock = ReentrantReadWriteLock()
  private var toolWindow: ToolWindow? = null
  private var deferredOpenToolTask: OpenToolTask<*>? = null // Also only used under `toolWindowLock`

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun setToolWindow(toolWindow: ToolWindow) {
    toolWindowLock.write {
      this.toolWindow = toolWindow
      if (deferredOpenToolTask != null) {
        doOpenTool(toolWindow, deferredOpenToolTask!!.context, deferredOpenToolTask!!.reference)
        deferredOpenToolTask = null
      }
    }
  }

  fun <T : OpenDeveloperToolContext> openTool(context: T, reference: OpenDeveloperToolReference<T>) {
    toolWindowLock.read {
      if (toolWindow != null) {
        toolWindow!!.show()
        doOpenTool(toolWindow!!, context, reference)
      }
      else {
        deferredOpenToolTask = OpenToolTask(context, reference)
        // The content of the tool window will be added asynchronous. So, the
        // return of `show()` (or its callback equivalents) will not indicate
        // the `toolWindowContentPanelHandlerKey` was already set. Therefore,
        // we have to defer the open tool task.
        ToolWindowManager.getInstance(project).getToolWindow(MainToolWindowFactory.ID)?.show()
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun <T : OpenDeveloperToolContext> doOpenTool(
    toolWindow: ToolWindow,
    context: T,
    reference: OpenDeveloperToolReference<out T>
  ) {
    toolWindow.contentManager.getContent(0)
      ?.getUserData(toolWindowContentPanelHandlerKey)
      ?.openTool(context, reference)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private data class OpenToolTask<T : OpenDeveloperToolContext>(
    val context: T,
    val reference: OpenDeveloperToolReference<out T>
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val toolWindowContentPanelHandlerKey = Key<ContentPanelHandler>(ContentPanelHandler::class.simpleName!!)
  }
}