package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.regex

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.allowUiDslLabel
import org.intellij.lang.regexp.RegExpLanguage
import org.intellij.lang.regexp.intention.CheckRegExpForm
import java.lang.Boolean.TRUE

class RegexTextField(
  project: Project?,
  parentDisposable: Disposable,
  textProperty: ValueProperty<String>,
) : LanguageTextField(RegExpLanguage.INSTANCE, project, textProperty.get(), true) {

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  init {
    font = JBFont.create(font, false).biggerOn(1.4f)

    addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        textProperty.set(event.document.text, TEXT_PROPERTY_CHANGE_ID)
      }
    })

    allowUiDslLabel(this.component)

    textProperty.afterChangeConsumeEvent(parentDisposable) { event ->
      if (event.newValue != event.oldValue && event.id != TEXT_PROPERTY_CHANGE_ID) {
        text = event.newValue
      }
    }
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