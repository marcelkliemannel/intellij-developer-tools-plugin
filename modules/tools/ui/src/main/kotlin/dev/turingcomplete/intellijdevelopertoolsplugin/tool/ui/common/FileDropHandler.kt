package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.ide.dnd.FileCopyPasteUtil
import com.intellij.openapi.editor.EditorDropHandler
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.TransferHandler

class FileDropHandler(private val project: Project?, private val openFile: (Path) -> Unit) :
  TransferHandler(), EditorDropHandler, DropTargetListener {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun canImport(comp: JComponent?, transferFlavors: Array<out DataFlavor>?): Boolean {
    return canHandleDrop0(transferFlavors)
  }

  override fun canHandleDrop(transferFlavors: Array<out DataFlavor>): Boolean {
    return canHandleDrop0(transferFlavors)
  }

  override fun drop(event: DropTargetDropEvent) {
    event.acceptDrop(event.dropAction)
    handleDrop0(event.transferable)
  }

  override fun handleDrop(
    transferable: Transferable,
    project: Project?,
    editorWindow: EditorWindow?,
  ) {
    handleDrop0(transferable)
  }

  override fun importData(comp: JComponent?, transferable: Transferable): Boolean {
    return handleDrop0(transferable)
  }

  override fun dragEnter(dtde: DropTargetDragEvent?) {
    // Nothing to do
  }

  override fun dragOver(dtde: DropTargetDragEvent?) {
    // Nothing to do
  }

  override fun dropActionChanged(dtde: DropTargetDragEvent?) {
    // Nothing to do
  }

  override fun dragExit(dte: DropTargetEvent?) {
    // Nothing to do
  }

  private fun canHandleDrop0(transferFlavors: Array<out DataFlavor>?): Boolean {
    return transferFlavors != null && FileCopyPasteUtil.isFileListFlavorAvailable(transferFlavors)
  }

  private fun handleDrop0(transferable: Transferable): Boolean {
    try {
      FileCopyPasteUtil.getFiles(transferable)
        ?.takeIf { it.size == 1 }
        ?.let {
          openFile(it[0])
          return true
        }
    } catch (e: Exception) {
      Messages.showErrorDialog(project, "Failed to handle dropped file: ${e.message}.", "Open File")
    }
    return false
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
