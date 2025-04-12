package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.ThreadingAssertions.assertBackgroundThread
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool.Companion.DUMMY_DIALOG_VALIDATION_REQUESTOR
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.GeneralBundle
import java.awt.dnd.DropTarget
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.swing.JComponent
import javax.swing.SwingConstants

class FileHandling(
  val project: Project?,
  val file: ValueProperty<String> = ValueProperty<String>(""),
) {
  // -- Properties ---------------------------------------------------------- //

  private val lastWriteInformation =
    ValueProperty<String>(GeneralBundle.message("file-handling.last-write.never"))

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun crateComponent(errorHolder: ErrorHolder): JComponent = panel {
    row {
        val fileChooserDescriptor =
          FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle(GeneralBundle.message("file-handling.file-chooser.title"))
        textFieldWithBrowseButton(fileChooserDescriptor, project)
          .bindText(file)
          .validationOnApply(errorHolder.asValidation())
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
          .resizableColumn()
      }
      .bottomGap(BottomGap.NONE)

    row { label("").bindText(lastWriteInformation).resizableColumn() }
      .topGap(TopGap.NONE)
      .bottomGap(BottomGap.SMALL)

    row {
        cell(
            BorderLayoutPanel().apply {
              addToCenter(
                JBLabel(
                  GeneralBundle.message("file-handling.drop-file-here"),
                  AllIcons.Actions.Download,
                  SwingConstants.CENTER,
                )
              )
              border = UIUtil.getTextFieldBorder()
              dropTarget = DropTarget(this, FileDropHandler(project) { file.set(it.toString()) })
            }
          )
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  fun readFromFile(): ByteArray {
    assertBackgroundThread()

    val fileToUse = Paths.get(file.get())

    checkFile(fileToUse)
    if (!Files.isReadable(fileToUse)) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.file-is-not-readable"))
    }

    return Files.readAllBytes(fileToUse)
  }

  fun writeToFile(bytes: ByteArray) {
    assertBackgroundThread()

    val fileToUse = Paths.get(file.get())

    checkFile(fileToUse)
    if (!Files.isWritable(fileToUse)) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.file-is-not-writable"))
    }

    try {
      Files.write(
        fileToUse,
        bytes,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
      )
      lastWriteInformation.set(
        GeneralBundle.message(
          "file-handling.last-write.details",
          DateFormatUtil.formatDateTime(System.currentTimeMillis()),
          StringUtil.formatFileSize(bytes.size.toLong()),
        )
      )
    } catch (e: Exception) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.failed-to-write"), e)
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun checkFile(file: Path?) {
    if (file == null || file.toString().isBlank()) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.no-file-selected"))
    } else if (!Files.exists(file)) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.file-not-exist"))
    } else if (Files.isDirectory(file)) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.file-is-directory"))
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
