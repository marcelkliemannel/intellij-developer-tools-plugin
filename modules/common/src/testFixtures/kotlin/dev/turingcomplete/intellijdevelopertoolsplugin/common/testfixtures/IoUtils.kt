package dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

object IoUtils {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun File.collectAllFiles(): List<File> {
    check(this.isDirectory)

    val files = mutableListOf<File>()

    this.listFiles()?.forEach { file ->
      if (file.isDirectory) {
        files.addAll(file.collectAllFiles())
      } else {
        files.add(file)
      }
    }

    return files
  }

  fun Path.collectAllFiles(): List<Path> {
    check(this.isDirectory())

    return Files.walk(this).filter { it.isRegularFile() }.toList()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
