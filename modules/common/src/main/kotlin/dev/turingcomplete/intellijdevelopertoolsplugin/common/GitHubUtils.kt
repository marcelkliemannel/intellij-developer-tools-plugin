package dev.turingcomplete.intellijdevelopertoolsplugin.common

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.common.OkHttpClientUtils.applyIntelliJProxySettings
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object GitHubUtils {
  // -- Properties ---------------------------------------------------------- //

  private val log = logger<GitHubUtils>()

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun downloadFiles(
    project: Project,
    repositoryUrl: String,
    destinationPath: Path,
    preDownloadFilter: (String) -> Boolean,
    afterDownloadFilter: (Path) -> Boolean,
    onStart: () -> Unit,
    onSuccess: () -> Unit,
    onThrowable: (Throwable) -> Unit,
    onFinished: () -> Unit
  ) = object : Task.Backgroundable(project, "Downloading files from GitHub") {

    override fun run(indicator: ProgressIndicator) {
      onStart()
      indicator.isIndeterminate = true

      val httpClient = OkHttpClient.Builder()
        .applyIntelliJProxySettings(repositoryUrl)
        .build()

      val fileNamesToDownloadUrls = fetchFiles(httpClient, repositoryUrl).filter { preDownloadFilter(it.key) }
      indicator.isIndeterminate = false

      var index = 1
      var errors = 0
      fileNamesToDownloadUrls.forEach { fileName, downloadUrl ->
        if (indicator.isCanceled) {
          return@forEach
        }

        indicator.text = "Downloading $fileName"
        indicator.fraction = index.toDouble() / fileNamesToDownloadUrls.size

        val targetPath = destinationPath.resolve(fileName)
        val success = downloadFile(httpClient, downloadUrl, targetPath)
        if (!success) {
          errors++
        }
        else {
          indicator.text = "Analyzing $fileName"
          val keepFile = afterDownloadFilter(targetPath)
          if (!keepFile) {
            try {
              Files.deleteIfExists(targetPath)
            }
            catch (e: Exception) {
              log.warn("Failed to delete file: $targetPath", e)
            }
          }
        }
        index++
      }
      if (errors > 0) {
        throw Exception("Failed to download $errors of ${fileNamesToDownloadUrls.size} files")
      }
      indicator.text = "All files downloaded"
    }

    override fun onThrowable(error: Throwable) {
      onThrowable(error)
    }

    override fun onFinished() {
      onFinished()
    }

    override fun onSuccess() {
      onSuccess()
    }
  }

  private fun fetchFiles(httpClient: OkHttpClient, repositoryUrl: String): Map<String, String> {
    val apiUrl = repositoryUrl.replace("https://github.com/", "https://api.github.com/repos/") + "/contents"
    val request = Request.Builder()
      .get()
      .url(apiUrl)
      .header("Accept", "application/vnd.github.v3+json")
      .build()

    val response = httpClient.newCall(request).execute()
    if (response.code == 200) {
      response.body?.byteStream()?.use { inputStream ->
        val rootNode: JsonNode = ObjectMapper().readTree(inputStream)
        return rootNode
          .filter { it["type"].asText() == "file" }
          .associate { it["name"].asText() to it["download_url"].asText() }
      }
      throw Exception("No HTTP body received from $apiUrl")
    }
    else {
      throw Exception("Failed to fetch file list from $apiUrl: HTTP ${response.code}")
    }
  }

  private fun downloadFile(
    httpClient: OkHttpClient,
    downloadUrl: String,
    targetPath: Path
  ): Boolean {
    val request = Request.Builder()
      .get()
      .url(downloadUrl)
      .build()

    log.info("Downloading $downloadUrl to $targetPath")

    val response = httpClient.newCall(request).execute()
    return if (response.code == 200) {
      response.body?.byteStream()?.use { inputStream ->
        Files.createDirectories(targetPath.parent)
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        true
      } == true
    }
    else {
      log.warn("Failed to download $downloadUrl to $targetPath: HTTP ${response.code}")
      false
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
