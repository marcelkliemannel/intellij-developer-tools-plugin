package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.panel.ComponentPanelBuilder
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.ScalableIcon
import com.intellij.ui.ClickListener
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.SizedIcon
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.LayoutManager
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LabelUI

internal object UiUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val MONOSPACE_FONT: JBFont = JBFont.create(Font(Font.MONOSPACED, Font.PLAIN, JBFont.label().size))

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun createFillPanel() = BorderLayoutPanel().apply { border = JBUI.Borders.empty() }

  fun createJBPanel(layoutManager: LayoutManager) = JBPanel<JBPanel<*>>(layoutManager)

  fun createDefaultGridBag() = GridBag()
          .setDefaultAnchor(GridBagConstraints.NORTHWEST)
          .setDefaultInsets(0, 0, 0, 0)
          .setDefaultFill(GridBagConstraints.NONE)

  fun createCopyToClipboardButton(value: () -> String) = object : JLabel(AllIcons.Actions.Copy) {

    init {
      object : ClickListener() {
        override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
          CopyPasteManager.getInstance().setContents(StringSelection(value()))
          return true
        }
      }.installOn(this)

      toolTipText = "Copy to Clipboard"
    }
  }

  fun createCommentLabel(text: String) = object : JBLabel(text) {

    init {
      foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
    }

    override fun setUI(ui: LabelUI?) {
      super.setUI(ui)
      font = ComponentPanelBuilder.getCommentFont(font)
    }
  }

  fun createLink(title: String, url: String): HyperlinkLabel {
    return HyperlinkLabel(title).apply {
      setHyperlinkTarget(url)
    }
  }

  fun createSimpleToggleAction(text: String, icon: Icon?, isSelected: () -> Boolean, setSelected: (Boolean) -> Unit): ToggleAction {

    return object : DumbAwareToggleAction(text, "", icon) {

      override fun isSelected(e: AnActionEvent): Boolean = isSelected.invoke()

      override fun setSelected(e: AnActionEvent, state: Boolean) = setSelected.invoke(state)

      override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
  }

  fun createSeparator(title: String) = SeparatorWithText().apply {
    caption = title
    setCaptionCentered(false)
  }

  fun createContextHelpLabel(text: String) = JLabel(AllIcons.General.ContextHelp).apply {
    toolTipText = text
  }

  fun loadPluginIcon(iconFileName: String, width: Int = 16, height: Int = 16): ScalableIcon {
    val icon = IconLoader.getIcon("dev/turingcomplete/intellijdevelopertoolsplugin/icons/${iconFileName}", UiUtils::class.java)
    return JBUIScale.scaleIcon(SizedIcon(icon, width, height))
  }

  fun createActionButton(name: String?, icon: Icon? = null, action: (ActionEvent) -> Unit) = JButton().apply {
    setAction(object : AbstractAction(name, icon) {
      override fun actionPerformed(e: ActionEvent) {
        action(e)
      }
    })
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}

fun GridBag.overrideLeftInset(leftInset: Int): GridBag {
  this.insets(this.insets.top, leftInset, this.insets.bottom, this.insets.right)
  return this
}

fun GridBag.overrideRightInset(rightInset: Int): GridBag {
  this.insets(this.insets.top, this.insets.left, this.insets.bottom, rightInset)
  return this
}

fun GridBag.overrideBottomInset(bottomInset: Int): GridBag {
  this.insets(this.insets.top, this.insets.left, bottomInset, this.insets.right)
  return this
}

fun GridBag.overrideTopInset(topInset: Int): GridBag {
  this.insets(topInset, this.insets.left, this.insets.bottom, this.insets.right)
  return this
}

fun JBLabel.copyable() = this.apply { setCopyable(true) }

fun Font.sizeToXl(): Font = this.deriveFont(this.size + JBUIScale.scale(2f))

fun Font.sizeToXxl(): Font = this.deriveFont(this.size + JBUIScale.scale(4f))

fun JComponent.wrapWithToolBar(actionEventPlace: String, actions: ActionGroup, toolBarPlace: ToolBarPlace, withBorder: Boolean = true): JComponent {
  return BorderLayoutPanel().apply {
    val actionToolbar = ActionManager.getInstance().createActionToolbar(actionEventPlace, actions, toolBarPlace.horizontal)
    actionToolbar.targetComponent = this@wrapWithToolBar

    val component = this@wrapWithToolBar.apply {
      if (withBorder) {
        border = BorderFactory.createEtchedBorder()
      }
    }
    when (toolBarPlace) {
      ToolBarPlace.LEFT -> {
        addToLeft(actionToolbar.component)
        addToCenter(component)
      }

      ToolBarPlace.RIGHT, ToolBarPlace.APPEND -> {
        addToCenter(component)
        addToRight(actionToolbar.component)
      }
    }
  }
}

enum class ToolBarPlace(val horizontal: Boolean) {

  LEFT(false),
  RIGHT(false),
  APPEND(true)
}

fun JComponent.bindToRadioButton(radioButton: JBRadioButton) {
  radioButton.addItemListener {
    this.isEnabled = radioButton.isSelected
  }

  this.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent?) {
      radioButton.isSelected = true
    }
  })
}

fun JBRadioButton.onSelected(selectListener: () -> Unit) = this.apply {
  this.addItemListener { event ->
    if (event.stateChange == ItemEvent.SELECTED) {
      selectListener()
    }
  }
}

fun JBCheckBox.onSelectionChanged(selectionChangedListener: (Boolean) -> Unit) = this.apply {
  this.addItemListener { event ->
    if (event.stateChange == ItemEvent.SELECTED) {
      selectionChangedListener(true)
    }
    else if (event.stateChange == ItemEvent.DESELECTED) {
      selectionChangedListener(false)
    }
  }
}

fun <T> ComboBox<T>.onChanged(changeListener: (T) -> Unit) {
  this.addItemListener { event ->
    if (event.stateChange == ItemEvent.SELECTED) {
      @Suppress("UNCHECKED_CAST")
      changeListener(selectedItem as T)
    }
  }
}