package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.DataGenerators.DataGenerator
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.DataGenerators.DataGeneratorBase
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.DataGenerators.DataGeneratorsGroup
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.DataGenerators.dataGenerators
import dev.turingcomplete.intellijdevelopertoolsplugin.common.EditorUtils.executeWriteCommand

internal class DataGeneratorActionGroup : DefaultActionGroup("Insert Generated Data", true) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val dataGeneratorActions: Array<AnAction> = dataGenerators.map { createDataGeneratorAction(it) }.toTypedArray()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun getChildren(e: AnActionEvent?): Array<AnAction> = dataGeneratorActions

  override fun update(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR)
    e.presentation.isVisible = editor != null && editor.document.isWritable
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createDataGeneratorAction(dataGeneratorBase: DataGeneratorBase): AnAction = when(dataGeneratorBase) {
    is DataGenerator -> DataGeneratorAction(dataGeneratorBase)
    is DataGeneratorsGroup -> object: DefaultActionGroup(dataGeneratorBase.title, true) {

      override fun getChildren(e: AnActionEvent?): Array<AnAction> =
        dataGeneratorBase.children.map { createDataGeneratorAction(it) }.toTypedArray()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class DataGeneratorAction(
    private val dataGenerator: DataGenerator
  ) : DumbAwareAction(dataGenerator.title, dataGenerator.toolText, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: return
      editor.executeWriteCommand(dataGenerator.actionName) {
        val result = dataGenerator.generate()
        val selectionStart = editor.selectionModel.selectionStart
        val selectionEnd = editor.selectionModel.selectionEnd
        if (selectionEnd > selectionStart) {
          it.document.replaceString(selectionStart, selectionEnd, result)
        }
        else {
          val currentOffset = editor.caretModel.offset
          it.document.insertString(currentOffset, result)
          editor.caretModel.moveToOffset(currentOffset + result.length)
        }
      }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}