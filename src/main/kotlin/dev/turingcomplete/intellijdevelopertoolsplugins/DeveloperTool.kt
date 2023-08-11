package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ComponentUtil.findComponentsOfType
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Dimension
import javax.swing.JComponent

abstract class DeveloperTool(
  protected val parentDisposable: Disposable
) : DataProvider, Disposable {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var panel: DialogPanel
  private val validationListeners = mutableSetOf<(List<ValidationInfo>) -> Unit>()
  var isDisposed = false
    private set

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun createComponent(): JComponent {
    panel = panel {
      buildUi()
    }
    // This prevents the `Editor` from increasing the size of the dialog if the
    // to display all the text on the screen instead of using scrollbars. The
    // reason for this behaviour is that the UI DSL always sets the minimum size
    // to the preferred size. But the preferred size gets calculated as if the
    // whole text gets displayed on the screen.
    panel.minimumSize = Dimension(0, 0)
    panel.withPreferredWidth(0)

    panel.registerValidators(parentDisposable)

    val wrapper = object : BorderLayoutPanel(), DataProvider {

      init {
        border = JBEmptyBorder(12, 16, 16, 16)
        addToCenter(panel)
      }

      override fun getData(dataId: String): Any? = this@DeveloperTool.getData(dataId)
    }

    afterBuildUi()

    return wrapper
  }

  abstract fun Panel.buildUi()

  open fun afterBuildUi() {
    // Override if needed
  }

  open fun activated() {
    // Override if needed
  }

  open fun deactivated() {
    // Override if needed
  }

  override fun getData(dataId: String): Any? {
    // Override if needed
    return null
  }

  final override fun dispose() {
    isDisposed = true
    doDispose()
  }

  open fun doDispose() {
    // Override if needed
  }

  open fun reset() {
    // Override if needed
  }

  fun validate(): List<ValidationInfo> {
    val result = findComponentsOfType(panel, DialogPanel::class.java).flatMap {
      it.validateAll()
    }.toList()
    validationListeners.forEach { it(result) }
    return result
  }

  fun registerValidationListeners(listener: (List<ValidationInfo>) -> Unit) {
    validationListeners.add(listener)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}