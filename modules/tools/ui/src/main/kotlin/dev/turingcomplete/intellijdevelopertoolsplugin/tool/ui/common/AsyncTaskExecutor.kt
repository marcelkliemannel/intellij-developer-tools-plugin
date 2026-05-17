package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.util.Disposer
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
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

  private val taskQueue = ConcurrentLinkedQueue<Pair<() -> Unit, Long>>()
  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val processing = AtomicBoolean(false)
  var isDisposed: Boolean = false
    private set

  // -- Initialization ------------------------------------------------------ //

  init {
    Disposer.register(parentDisposable, this)
  }

  // -- Exported Methods ---------------------------------------------------- //

  override fun dispose() {
    cancelAll()
    isDisposed = true
  }

  fun replaceTasks(delayMillis: Duration = Duration.ZERO, task: () -> Unit) {
    cancelAll()
    enqueueTask(delayMillis, task)
  }

  private fun enqueueTask(delayMillis: Duration = Duration.ZERO, task: () -> Unit) {
    if (isDisposed) {
      return
    }

    taskQueue.add(task to delayMillis.toMillis())
    processQueue()
  }

  fun cancelAll() {
    coroutineScope.coroutineContext.cancelChildren()
    taskQueue.clear()
    processing.set(false)
  }

  private fun processQueue() {
    if (!processing.compareAndSet(false, true)) {
      return
    }

    coroutineScope.launch {
      try {
        while (!isDisposed) {
          val item = taskQueue.poll() ?: break
          val (task, delayMillis) = item
          if (delayMillis > 0) {
            delay(delayMillis)
          }
          if (!isDisposed) {
            executeTask(task)
          }
        }
      } finally {
        processing.set(false)
        if (!isDisposed && taskQueue.isNotEmpty()) {
          processQueue()
        }
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private suspend fun executeTask(task: Runnable) {
    when (executionThread) {
      ExecutionThread.POOLED -> withContext(Dispatchers.IO) { task.run() }
      ExecutionThread.EDT ->
        withContext(Dispatchers.Main) {
          invokeLater(ModalityState.any()) {
            if (!isDisposed) {
              task.run()
            }
          }
        }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class ExecutionThread {
    POOLED,
    EDT,
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    val defaultUiInputDelay: Duration = Duration.ofMillis(50)

    fun onEdt(parentDisposable: Disposable) =
      AsyncTaskExecutor(parentDisposable, ExecutionThread.EDT)

    fun onPooled(parentDisposable: Disposable) =
      AsyncTaskExecutor(parentDisposable, ExecutionThread.POOLED)
  }
}
