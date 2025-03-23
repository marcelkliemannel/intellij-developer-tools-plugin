package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.regex

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import org.intellij.lang.regexp.RegExpLanguage
import org.intellij.lang.regexp.intention.CheckRegExpForm
import java.lang.Boolean.TRUE

class RegexTextField(
  project: Project?,
  parentDisposable: Disposable,
  textProperty: ValueProperty<String>,
) : LanguageTextField(RegExpLanguage.INSTANCE, project, textProperty.get(), true) {

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var onTextChangeFromUi = mutableListOf<((String) -> Unit)>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  init {
    font = JBFont.create(font, false).biggerOn(1.4f)

    addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        val text = event.document.text
        textProperty.set(text, TEXT_PROPERTY_CHANGE_ID)
        onTextChangeFromUi.forEach { it(text) }
      }
    })

    textProperty.afterChangeConsumeEvent(parentDisposable) { event ->
      if (event.newValue != event.oldValue && event.id != TEXT_PROPERTY_CHANGE_ID) {
        text = event.newValue
      }
    }
  }

  fun onTextChangeFromUi(changeListener: ((String) -> Unit)): RegexTextField {
    onTextChangeFromUi.add(changeListener)
    return this
  }

  override fun onEditorAdded(editor: Editor) {
    editor.putUserData(CheckRegExpForm.Keys.CHECK_REG_EXP_EDITOR, TRUE)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val TEXT_PROPERTY_CHANGE_ID = "RegexTextField"
  }
}
