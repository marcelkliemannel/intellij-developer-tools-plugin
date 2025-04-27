package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidationRequestor
import com.intellij.ui.ComponentUtil.findComponentsOfType
import com.intellij.ui.ScrollPaneFactory.createScrollPane
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Dimension
import javax.swing.JComponent

abstract class DeveloperUiTool(protected val parentDisposable: Disposable) :
  DataProvider, Disposable {
  // -- Properties ---------------------------------------------------------- //

  private lateinit var component: DialogPanel
  private val validationListeners = mutableSetOf<(List<ValidationInfo>) -> Unit>()
  var isDisposed = false
    private set

  protected var wrapComponentInScrollPane = true

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun createComponent(): JComponent {
    component = panel { buildUi() }
    // This prevents the `Editor` from increasing the size of the dialog if the
    // to display all the text on the screen instead of using scrollbars. The
    // reason for this behavior is that the UI DSL always sets the minimum size
    // to the preferred size. But the preferred size gets calculated as if the
    // whole text gets displayed on the screen.
    component.minimumSize = Dimension(0, 0)
    component.withPreferredWidth(0)

    findComponentsOfType(component, DialogPanel::class.java).forEach {
      it.registerValidators(parentDisposable)
    }

    var wrapper: JComponent =
      object : BorderLayoutPanel(), DataProvider {

        init {
          border = JBEmptyBorder(wrapperInsets())
          addToCenter(component)
        }

        override fun getData(dataId: String): Any? = this@DeveloperUiTool.getData(dataId)
      }

    if (wrapComponentInScrollPane) {
      wrapper = createScrollPane(wrapper, true)
    }

    afterBuildUi()

    return wrapper
  }

  open fun wrapperInsets(): JBInsets = JBUI.insets(12, 16, 16, 16)

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

  fun validate(onlyVisibleAndEnabled: Boolean = false): List<ValidationInfo> {
    val result =
      findComponentsOfType(component, DialogPanel::class.java)
        .filter { !onlyVisibleAndEnabled || (it.isVisible && it.isEnabled) }
        .flatMap { it.validateAll() }
        .toList()
    validationListeners.forEach { it(result) }
    return result
  }

  fun registerValidationListeners(listener: (List<ValidationInfo>) -> Unit) {
    validationListeners.add(listener)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    /**
     * If [com.intellij.ui.dsl.builder.Cell.validationOnApply] gets called and there is no
     * [com.intellij.ui.dsl.builder.Cell.validationRequestor] registered, IntelliJ will log the
     * warning `Please, install Cell.validationRequestor`. To circumvent this, we set a dummy
     * requestor without any functionally.
     *
     * Future todos: The validation mechanism of the new UI has been further developed since the
     * initial creation of the plugin. The current mechanism in [DeveloperUiTool.validate], may no
     * longer be necessary and may be better solved with on-board resources.
     */
    val DUMMY_DIALOG_VALIDATION_REQUESTOR = DialogValidationRequestor { _, _ -> }
  }
}
