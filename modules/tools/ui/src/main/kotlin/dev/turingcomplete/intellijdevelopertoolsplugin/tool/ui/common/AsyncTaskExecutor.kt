package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.util.Disposer
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AsyncTaskExecutor(
  parentDisposable: Disposable,
  private val executionThread: ExecutionThread,
) : Disposable {
  // -- Properties ---------------------------------------------------------- //

  private val taskQueue = ConcurrentLinkedQueue<Pair<Runnable, Long>>()
  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  var isDisposed: Boolean = false
    private set

  // -- Initialization ------------------------------------------------------ //

  init {
    Disposer.register(parentDisposable, this)
  }

  // -- Exported Methods ---------------------------------------------------- //

  override fun dispose() {
    isDisposed = true
    cancelAll()
  }

  fun enqueueTask(runnable: Runnable, delayMillis: Long = 0) {
    taskQueue.add(runnable to delayMillis)
    processQueue()
  }

  fun cancelAll() {
    coroutineScope.coroutineContext.cancelChildren()
    taskQueue.clear()
  }

  private fun processQueue() {
    coroutineScope.launch {
      while (taskQueue.isNotEmpty()) {
        val (task, delayMillis) = taskQueue.poll()
        if (delayMillis > 0) {
          delay(delayMillis)
        }
        executeTask(task)
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private suspend fun executeTask(task: Runnable) {
    when (executionThread) {
      ExecutionThread.POOLED -> withContext(Dispatchers.IO) { task.run() }
      ExecutionThread.EDT ->
        withContext(Dispatchers.Main) { invokeLater(ModalityState.any()) { task.run() } }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class ExecutionThread {
    POOLED,
    EDT,
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    fun onEdt(parentDisposable: Disposable) =
      AsyncTaskExecutor(parentDisposable, ExecutionThread.EDT)

    fun onPooled(parentDisposable: Disposable) =
      AsyncTaskExecutor(parentDisposable, ExecutionThread.POOLED)
  }
}
