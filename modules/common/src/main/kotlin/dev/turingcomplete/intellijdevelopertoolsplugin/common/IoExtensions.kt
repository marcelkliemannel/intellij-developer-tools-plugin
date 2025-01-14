package dev.turingcomplete.intellijdevelopertoolsplugin.common

import java.nio.file.Files
import java.nio.file.Path

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Initialization ------------------------------------------------------------------------------------------------ //
// -- Exported Methods ---------------------------------------------------------------------------------------------- //

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

fun Path.nameWithoutExtension() = fileName.toString().substringBeforeLast('.')

fun Path.extension() = fileName.toString().substringAfterLast('.')

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Inner Type ---------------------------------------------------------------------------------------------------- //