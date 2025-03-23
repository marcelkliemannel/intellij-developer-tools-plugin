package dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.intention

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.executeWriteCommand
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.DataGenerators.DataGenerator
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.DataGenerators.DataGeneratorBase
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.DataGenerators.DataGeneratorsGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.editor.DataGenerators.dataGenerators

class DataGeneratorIntentionAction : IntentionAction, LowPriorityAction {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun startInWriteAction(): Boolean = false

  override fun getFamilyName(): String = "Insert generated data"

  override fun getText(): String = familyName

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
    editor?.document?.isWritable == true

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (editor == null) {
      return
    }

    ApplicationManager.getApplication().invokeLater {
      JBPopupFactory.getInstance()
        .createListPopup(GenerateTextListPopupStep(dataGenerators, editor))
        .showInBestPositionFor(editor)
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class GenerateTextListPopupStep(
    dataGenerators: List<DataGeneratorBase>,
    private val editor: Editor,
  ) : BaseListPopupStep<DataGeneratorBase>(null, dataGenerators) {

    override fun getTextFor(dataGenerator: DataGeneratorBase): String = dataGenerator.title

    override fun isFinal(dataGenerator: DataGeneratorBase?): Boolean =
      dataGenerator is DataGenerator

    override fun hasSubstep(dataGenerator: DataGeneratorBase?): Boolean =
      dataGenerator is DataGeneratorsGroup

    override fun onChosen(dataGenerator: DataGeneratorBase, finalChoice: Boolean): PopupStep<*>? {
      when (dataGenerator) {
        is DataGenerator -> {
          editor.executeWriteCommand(dataGenerator.actionName) {
            val currentOffset = editor.caretModel.offset
            val result = dataGenerator.generate()
            it.document.insertString(currentOffset, result)
            editor.caretModel.moveToOffset(currentOffset + result.length)
          }

          return super.onChosen(dataGenerator, finalChoice)
        }

        is DataGeneratorsGroup -> {
          return GenerateTextListPopupStep(dataGenerator.children, editor)
        }
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
