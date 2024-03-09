package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaToken
import com.intellij.psi.util.elementType
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.EditorUtils.getSelectedText

/**
 * Some code parts of this class are only available of the optional dependency
 * `com.intellij.java` is available.
 */
internal object PsiJavaUtils {
  // -- Variables --------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun getTextFromStringValueOrIdentifier(e: AnActionEvent): Pair<String, TextRange>? {
    val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return null
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
    if (editor.getSelectedText() != null) {
      return null
    }

    val psiElement = psiFile.findElementAt(editor.caretModel.offset) ?: return null
    return getTextIfStringValueOrIdentifier(psiElement)?.let { it to psiElement.textRange }
  }

  fun getTextIfStringValueOrIdentifier(psiElement: PsiElement): String? {
    val isStringLiteral = psiElement is PsiJavaToken && psiElement.elementType == JavaTokenType.STRING_LITERAL
    return if (isStringLiteral || psiElement is PsiIdentifier) {
      psiElement.text
    }
    else {
      null
    }
  }
  
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}