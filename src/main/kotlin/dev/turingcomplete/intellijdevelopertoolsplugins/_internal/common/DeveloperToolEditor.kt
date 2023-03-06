package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.datatransfer.StringSelection
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants

internal class DeveloperToolEditor(
  private val title: String?,
  private val editorMode: EditorMode,
  private val parentDisposable: Disposable,
  initialLanguage: Language = PlainTextLanguage.INSTANCE
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var onTextChange: ((String) -> Unit)? = null
  private var onFocusGained: (() -> Unit)? = null
  private var onFocusLost: (() -> Unit)? = null

  private val editor: EditorEx by lazy { createEditor() }

  var text: String
    set(value) = runWriteAction {
      editor.document.setText(value)
    }
    get() = runReadAction {
      editor.document.text
    }

  var isDisposed: Boolean = false
    private set
    get() = editor.isDisposed

  var language: Language = initialLanguage
    set(value) {
      editor.setLanguage(value)
    }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun onTextChange(changeListener: ((String) -> Unit)?): DeveloperToolEditor {
    onTextChange = changeListener
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
        title?.let {
          addToTop(JBLabel("$it ${editorMode.title}:"))
        }
        val editorComponent = editor.component.wrapWithToolBar(DeveloperToolEditor::class.java.simpleName, createActions(), ToolBarPlace.RIGHT)
        addToCenter(editorComponent)
      }

      override fun getData(dataId: String): Any? = when {
        CommonDataKeys.EDITOR.`is`(dataId) -> editor
        else -> null
      }
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
    add(SaveContentToFile())
    if (editorMode.editable) {
      add(OpenContentFromFile())
    }
  }

  private fun createEditor(): EditorEx {
    val editorFactory = EditorFactory.getInstance()
    val document = editorFactory.createDocument("")
    val editor = if (editorMode.editable) {
      editorFactory.createEditor(document) as EditorEx
    }
    else {
      editorFactory.createViewer(document) as EditorEx
    }
    Disposer.register(parentDisposable) { EditorFactory.getInstance().releaseEditor(editor) }

    return editor.apply {
      document.addDocumentListener(TextChangeListener(), parentDisposable)
      addFocusListener(FocusListener(), parentDisposable)

      syncEditorColors()

      setBorder(JBUI.Borders.empty())
      setCaretVisible(true)
      markupModel.removeAllHighlighters()
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

  private fun EditorEx.setLanguage(language: Language) {
    val syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, null)
    highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(syntaxHighlighter, EditorColorsManager.getInstance().globalScheme)
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
      onTextChange?.invoke(currentText)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class FocusListener : FocusChangeListener {

    override fun focusGained(editor: Editor) {
      onFocusGained?.invoke()
    }

    override fun focusLost(editor: Editor) {
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
        editor.document.setText("")
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class CopyContentAction
    : DumbAwareAction("Copy to Clipboard", "Copy the text into the system clipboard", AllIcons.Actions.Copy) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val content = runReadAction { editor.document.text }
      CopyPasteManager.getInstance().setContents(StringSelection(content))
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class SaveContentToFile()
    : DumbAwareAction("Save to File", "Save the text into a file", AllIcons.Actions.MenuSaveall) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val fileSaverDescriptor = FileSaverDescriptor("Save Content As", "")
      val timeStamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
      val defaultFilename = "$timeStamp.txt"
      FileChooserFactory.getInstance()
              .createSaveFileDialog(fileSaverDescriptor, e.project)
              .save(defaultFilename)?.file?.toPath()?.let {
                val content = runReadAction { editor.document.text }
                Files.writeString(it, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
              }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpenContentFromFile
    : DumbAwareAction("Copy to Clipboard", "Copy the text into the system clipboard", AllIcons.Actions.MenuOpen) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val fileChooserDescriptor = FileChooserDescriptor(true, true, false, false, false, false)
      FileChooserFactory.getInstance()
              .createFileChooser(fileChooserDescriptor, e.project, editor.component)
              .choose(e.project).first()?.let {
                runWriteAction {
                  editor.document.setText(Files.readString(it.toNioPath()))
                }
                editor.contentComponent.grabFocus()
              }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class EditorMode(val title: String, val editable: Boolean) {

    INPUT("input", true),
    OUTPUT("output", false),
    INPUT_OUTPUT("input/output", true),
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-SS")
  }
}