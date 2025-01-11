package dev.turingcomplete.intellijdevelopertoolsplugin.javadependent

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaToken
import com.intellij.psi.util.elementType
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.getSelectedText

internal object PsiJavaUtils {
  // -- Variables --------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun getPsiElementAtCaret(e: AnActionEvent): PsiElement? {
    val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return null
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
    if (editor.getSelectedText() != null) {
      return null
    }

    return psiFile.findElementAt(editor.caretModel.offset)
  }

  fun getTextIfStringValueOrIdentifier(psiElement: PsiElement): Pair<String, TextRange>? {
    return getTextIfStringValue(psiElement)
      ?: if (psiElement is PsiIdentifier) {
        psiElement.text to psiElement.textRange
      }
      else {
        null
      }
  }

  fun getTextIfStringValue(psiElement: PsiElement): Pair<String, TextRange>? =
    if (psiElement is PsiJavaToken && psiElement.elementType == JavaTokenType.STRING_LITERAL) {
      // Remove the enclosing quotations
      val newStart = psiElement.textRange.startOffset + 1
      val newEnd = psiElement.textRange.endOffset - 1
      if (newStart > newEnd) {
        null
      }
      else {
        psiElement.text.substring(1, psiElement.text.length - 1) to TextRange(newStart, newEnd)
      }
    }
    else {
      null
    }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}