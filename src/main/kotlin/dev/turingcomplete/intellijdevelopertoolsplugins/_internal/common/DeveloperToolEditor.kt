@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ex.ClipboardUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.editor.impl.EditorFactoryImpl
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.UiUtils.actionsPopup
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.UiUtils.dumbAwareAction
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty
import java.awt.datatransfer.StringSelection
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants

internal class DeveloperToolEditor(
  private val title: String? = null,
  private val editorMode: EditorMode,
  private val parentDisposable: Disposable,
  private val textProperty: ValueProperty<String> = ValueProperty(""),
  initialLanguage: Language = PlainTextLanguage.INSTANCE,
  private val diffSupport: DiffSupport? = null
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var onTextChangeFromUi = mutableListOf<((String) -> Unit)>()
  private var onFocusGained: (() -> Unit)? = null
  private var onFocusLost: (() -> Unit)? = null
  private var rangeHighlighters = mutableMapOf<String, MutableList<RangeHighlighter>>()

  private val editor: EditorEx by lazy { createEditor() }

  var text: String
    set(value) = textProperty.set(value)
    get() = textProperty.get()

  var isDisposed: Boolean = false
    private set
    get() = editor.isDisposed

  var language: Language = initialLanguage
    set(value) {
      editor.setLanguage(value)
    }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    textProperty.afterChangeConsumeEvent(parentDisposable) { event ->
      if (event.id != TEXT_CHANGE_FROM_DOCUMENT_LISTENER) {
        runWriteAction { editor.document.setText(event.newValue) }
      }
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun onTextChangeFromUi(changeListener: ((String) -> Unit)): DeveloperToolEditor {
    onTextChangeFromUi.add(changeListener)
    return this
  }

  fun onFocusGained(changeListener: () -> Unit): DeveloperToolEditor {
    onFocusGained = changeListener
    return this
  }

  fun onFocusLost(changeListener: () -> Unit): DeveloperToolEditor {
    onFocusLost = changeListener
    return this
  }

  fun createComponent(): JComponent =
    object : BorderLayoutPanel(0, UIUtil.DEFAULT_VGAP), DataProvider {
      init {
        title?.let { addToTop(JBLabel("$it ${editorMode.title}:")) }

        editor.component.border = ValidationResultBorder(editor.component, editor.contentComponent)
        val editorComponent = editor.component.wrapWithToolBar(DeveloperToolEditor::class.java.simpleName, createActions(), ToolBarPlace.RIGHT)
        addToCenter(editorComponent)
        // This prevents the `Editor` from increasing the size of the dialog if
        // the to display all the text on the screen instead of using scrollbars.
        preferredSize = JBUI.size(0, 120)
        minimumSize = JBUI.size(0, 50)
      }

      override fun getData(dataId: String): Any? = when {
        CommonDataKeys.EDITOR.`is`(dataId) -> editor
        else -> null
      }
    }

  fun <T> bindValidator(validation: ValidationInfoBuilder.(T) -> ValidationInfo?): ValidationInfoBuilder.(T) -> ValidationInfo? = {
    validation(it)?.forComponent(editor.component)
  }

  fun removeAllTextRangeHighlighters() {
    editor.markupModel.removeAllHighlighters()
    rangeHighlighters.clear()
  }

  fun removeTextRangeHighlighters(groupId: String) {
    rangeHighlighters[groupId]?.forEach { editor.markupModel.removeHighlighter(it) }
    rangeHighlighters.remove(groupId)
  }

  fun highlightTextRange(
    textRange: TextRange,
    layer: Int,
    textAttributes: TextAttributes?,
    groupId: String = "other",
    gutterIconRenderer: GutterIconRenderer? = null,
  ) {
    val rangeHighlighter = editor.markupModel.addRangeHighlighter(
      textRange.startOffset,
      textRange.endOffset,
      layer,
      textAttributes,
      HighlighterTargetArea.EXACT_RANGE
    )
    rangeHighlighter.gutterIconRenderer = gutterIconRenderer
    rangeHighlighters.computeIfAbsent(groupId) { mutableListOf() }.add(rangeHighlighter)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createActions() = DefaultActionGroup().apply {
    add(CopyContentAction())
    if (editorMode.editable) {
      add(ClearContentAction())
    }
    addSeparator()
    add(SimpleToggleAction(
      text = "Soft-Wrap",
      icon = AllIcons.Actions.ToggleSoftWrap,
      isSelected = { editor.settings.isUseSoftWraps },
      setSelected = { editor.settings.isUseSoftWraps = it }
    ))
    addSeparator()
    val additionalActions = mutableListOf<AnAction>().apply {
      addAll(createDiffAction())
      add(Separator.getInstance())
      add(SaveContentToFile())
      if (editorMode.editable) {
        add(OpenContentFromFile())
      }
    }
    add(
      actionsPopup(
        title = "Additional Actions",
        icon = AllIcons.Actions.MoreHorizontal,
        actions = additionalActions
      )
    )
  }

  private fun createDiffAction(): List<AnAction> {
    val actions = mutableListOf<AnAction>()

    val firstTitle = title ?: "Content"

    actions.add(dumbAwareAction("Show Diff with Clipboard", AllIcons.Actions.DiffWithClipboard) { e ->
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val firstText = runReadAction { editor.document.text }
      UiUtils.showDiffDialog(
        title = "Show Diff with Clipboard",
        firstTitle = firstTitle,
        secondTitle = "Clipboard",
        firstText = firstText,
        secondText = ClipboardUtil.getTextInClipboard() ?: ""
      )
    })

    diffSupport?.let {
      actions.add(dumbAwareAction("Show Diff with ${it.secondTitle}", AllIcons.Actions.Diff) { e ->
        val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
        val firstText = runReadAction { editor.document.text }
        UiUtils.showDiffDialog(
          title = "Show Diff with ${it.secondTitle}",
          firstTitle = firstTitle,
          secondTitle = it.secondTitle,
          firstText = firstText,
          secondText = it.secondText()
        )
      })
    }

    return actions
  }

  private fun createEditor(): EditorEx {
    val editorFactory = EditorFactory.getInstance()
    val document = (editorFactory as EditorFactoryImpl).createDocument(textProperty.get(), true, false)
    val editor = if (editorMode.editable) {
      editorFactory.createEditor(document) as EditorEx
    }
    else {
      editorFactory.createViewer(document) as EditorEx
    }
    editor.putUserData(editorActiveKey, UIUtil.hasFocus(editor.contentComponent))
    Disposer.register(parentDisposable) { EditorFactory.getInstance().releaseEditor(editor) }

    return editor.apply {
      document.addDocumentListener(TextChangeListener(), parentDisposable)
      addFocusListener(FocusListener(), parentDisposable)

      syncEditorColors()

      setBorder(JBUI.Borders.empty())
      setCaretVisible(true)
      scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
      scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS

      setLanguage(language)

      settings.apply {
        isLineMarkerAreaShown = true
        isIndentGuidesShown = true
        isLineNumbersShown = true
        isFoldingOutlineShown = false
        isUseSoftWraps = true
        isBlinkCaret = editorMode.editable
        additionalLinesCount = 0
      }
    }
  }

  private fun EditorEx.syncEditorColors() {
    setBackgroundColor(null) // To use background from set color scheme

    val isLaFDark = ColorUtil.isDark(UIUtil.getPanelBackground())
    val isEditorDark = EditorColorsManager.getInstance().isDarkEditor
    colorsScheme = if (isLaFDark == isEditorDark) {
      EditorColorsManager.getInstance().globalScheme
    }
    else {
      EditorColorsManager.getInstance().schemeForCurrentUITheme
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class TextChangeListener : DocumentListener {

    override fun documentChanged(event: DocumentEvent) {
      val currentText = event.document.text

      textProperty.set(value = currentText, changeId = TEXT_CHANGE_FROM_DOCUMENT_LISTENER)

      if (editor.getUserData(editorActiveKey)!!) {
        onTextChangeFromUi.forEach { it(currentText) }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class FocusListener : FocusChangeListener {

    override fun focusGained(editor: Editor) {
      editor.putUserData(editorActiveKey, true)
      editor.component.repaint()
      onFocusGained?.invoke()
    }

    override fun focusLost(editor: Editor) {
      editor.putUserData(editorActiveKey, false)
      editor.component.repaint()
      onFocusLost?.invoke()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ClearContentAction
    : DumbAwareAction("Clear", "Clear the content", AllIcons.Actions.GC) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      editor.contentComponent.grabFocus()
      runWriteAction {
        editor.putUserData(editorActiveKey, true)
        editor.document.setText("")
      }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class CopyContentAction
    : DumbAwareAction("Copy to Clipboard", "Copy the text into the system clipboard", AllIcons.Actions.Copy) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val content = runReadAction { editor.document.text }
      CopyPasteManager.getInstance().setContents(StringSelection(content))
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class SaveContentToFile
    : DumbAwareAction("Save to File", "Save the text into a file", AllIcons.Actions.MenuSaveall) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val fileSaverDescriptor = FileSaverDescriptor("Save Content As", "")
      val timeStamp = LocalDateTime.now().format(timestampFormat)
      val defaultFilename = "$timeStamp.txt"
      FileChooserFactory.getInstance()
        .createSaveFileDialog(fileSaverDescriptor, e.project)
        .save(defaultFilename)?.file?.toPath()?.let {
          val content = runReadAction { editor.document.text }
          Files.writeString(it, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpenContentFromFile
    : DumbAwareAction("Open from File", "Replaces the text with the content of a file", AllIcons.Actions.MenuOpen) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val fileChooserDescriptor = FileChooserDescriptor(true, true, false, false, false, false)
      FileChooserFactory.getInstance()
        .createFileChooser(fileChooserDescriptor, e.project, editor.component)
        .choose(e.project).first()?.let {
          runWriteAction {
            editor.putUserData(editorActiveKey, true)
            editor.document.setText(Files.readString(it.toNioPath()))
          }
          editor.contentComponent.grabFocus()
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class DiffSupport(
    val title: String,
    val secondTitle: String,
    val secondText: () -> String
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class EditorMode(val title: String, val editable: Boolean) {

    INPUT("input", true),
    OUTPUT("output", false),
    INPUT_OUTPUT("input/output", true),
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val TEXT_CHANGE_FROM_DOCUMENT_LISTENER = "documentChangeListener"

    private val editorActiveKey = Key<Boolean>("editorActive")
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-SS")
  }
}