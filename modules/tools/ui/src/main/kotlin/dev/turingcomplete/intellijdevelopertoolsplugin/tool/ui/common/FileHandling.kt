package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.ThreadingAssertions.assertBackgroundThread
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.toHexString
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
  val file: ValueProperty<String> = ValueProperty(""),
  val writeFormat: ValueProperty<WriteFormat> = ValueProperty(WriteFormat.BINARY),
  val supportsWrite: Boolean,
) {
  // -- Properties ---------------------------------------------------------- //

  private val lastWriteInformation =
    ValueProperty(GeneralBundle.message("file-handling.last-write.never"))

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun crateComponent(errorHolder: ErrorHolder): JComponent = panel {
    row {
        // Allow the selection of folders so that the user can manually extend the path with a file
        // name, of the file does not exist yet.
        val fileChooserDescriptor =
          FileChooserDescriptor(true, true, false, false, false, false)
            .withTitle(GeneralBundle.message("file-handling.file-chooser.title"))
        textFieldWithBrowseButton(fileChooserDescriptor, project)
          .bindText(file)
          .validationOnApply(errorHolder.asValidation())
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
          .resizableColumn()
      }
      .bottomGap(if (supportsWrite) BottomGap.NONE else BottomGap.SMALL)

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

    if (supportsWrite) {
      row {
        label(GeneralBundle.message("file-handling.write-format.title")).gap(RightGap.SMALL)
        segmentedButton(WriteFormat.entries) { text = it.title }
          .bind(writeFormat)
          .gap(RightGap.SMALL)
        contextHelp(
          WriteFormat.entries.joinToString(separator = "", prefix = "<ul>", postfix = "</ul>") {
            "<li><b>${it.title}</b>: ${it.description}</li>"
          }
        )
      }

      row { comment("").bindText(lastWriteInformation).resizableColumn() }
    }
  }

  fun readFromFile(): ByteArray {
    assertBackgroundThread()

    val fileToUse = Paths.get(file.get())

    checkFile(fileToUse)

    if (!Files.exists(fileToUse)) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.file-not-exist"))
    } else if (!Files.isReadable(fileToUse)) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.file-is-not-readable"))
    }

    return Files.readAllBytes(fileToUse)
  }

  fun writeToFile(bytes: ByteArray) {
    check(supportsWrite)
    assertBackgroundThread()

    val fileToUse = Paths.get(file.get())

    checkFile(fileToUse)
    if (Files.exists(fileToUse) && !Files.isWritable(fileToUse)) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.file-is-not-writable"))
    }

    val content =
      when (writeFormat.get()) {
        WriteFormat.BINARY -> bytes
        WriteFormat.HEX -> bytes.toHexString().encodeToByteArray()
      }

    try {
      Files.write(
        fileToUse,
        content,
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
    } else if (Files.isDirectory(file)) {
      throw IllegalStateException(GeneralBundle.message("file-handling.error.file-is-directory"))
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class WriteFormat(val title: String, val description: String) {
    BINARY(
      GeneralBundle.message("file-handling.write-format.binary.title"),
      GeneralBundle.message("file-handling.write-format.binary.description"),
    ),
    HEX(
      GeneralBundle.message("file-handling.write-format.hex.title"),
      GeneralBundle.message("file-handling.write-format.hex.description"),
    ),
  }

  // -- Companion Object ---------------------------------------------------- //
}
