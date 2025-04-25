package dev.turingcomplete.intellijdevelopertoolsplugin.common

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

// -- Properties ---------------------------------------------------------- //
// -- Initialization ------------------------------------------------------ //
// -- Exported Methods ---------------------------------------------------- //

fun Path.clearDirectory() {
  if (!Files.exists(this) || !Files.isDirectory(this)) {
    return
  }
  Files.newDirectoryStream(this).use { stream ->
    for (path in stream) {
      if (Files.isDirectory(path)) {
        path.clearDirectory()
        Files.delete(path)
      } else {
        Files.delete(path)
      }
    }
  }
}

fun Path.nameWithoutExtension(): String = fileName.toString().substringBeforeLast('.')

fun Path.extension(): String = fileName.toString().substringAfterLast('.')

fun InputStream.readProperties(): Properties = this.use { Properties().apply { load(it) } }

// -- Private Methods  ---------------------------------------------------- //
// -- Inner Type ---------------------------------------------------------- //
