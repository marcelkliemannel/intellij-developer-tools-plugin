package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.json.JsonLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.OkHttpClientUtils.applyIntelliJProxySettings
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.ObjectMapperService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.applyDefaultTabComponentInsets
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bind
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

class HttpServer(
  private val configuration: DeveloperToolConfiguration,
  private val project: Project?,
  private val context: DeveloperUiToolContext,
  parentDisposable: Disposable,
) : DeveloperUiTool(parentDisposable) {
  // -- Properties ---------------------------------------------------------- //

  private val log = logger<HttpServer>()
  private val content = BorderLayoutPanel()
  private val serverStatus =
    ValueProperty(UiToolsBundle.message("http-server.server.status.stopped"))
  private val serverUrl = ValueProperty("")
  private val serverRunning = ValueProperty(false)
  private val serverBusy = ValueProperty(false)
  private val restartRequired = ValueProperty(false)

  private val serverPort =
    configuration.register("serverPort", DEFAULT_WIREMOCK_PORT, CONFIGURATION)
  private val verboseLogging = configuration.register("verboseLogging", false, CONFIGURATION)
  private val printAllNetworkTraffic =
    configuration.register("printAllNetworkTraffic", false, CONFIGURATION)
  private val advancedCommandLineOptions =
    configuration.register(
      "advancedCommandLineOptions",
      DEFAULT_ADVANCED_COMMAND_LINE_OPTIONS,
      CONFIGURATION,
    )
  private val javaExecutableMode =
    configuration.register("javaExecutableMode", JavaExecutableMode.BUILT_IN_JRE, CONFIGURATION)
  private val javaExecutablePath = configuration.register("javaExecutablePath", "", CONFIGURATION)
  private val serverMode = configuration.register("serverMode", ServerMode.BUILT_IN_SERVER)
  private val customRootDirectory = configuration.register("customRootDirectory", "", CONFIGURATION)
  private val builtInServerMapping =
    configuration.register(
      "builtInServerMapping",
      "",
      CONFIGURATION,
      """
        {
          "request": {
            "method": "GET",
            "urlPattern": "/"
          },
          "response": {
            "status": 200,
            "headers": {
              "Content-Type": "text/plain"
            },
            "body": "Hello World!"
          }
        }
      """
        .trimIndent(),
    )

  private val builtInServerMappingEditor =
    AdvancedEditor(
      id = "builtInServerMapping",
      context = context,
      configuration = configuration,
      project = project,
      title = null,
      editorMode = AdvancedEditor.EditorMode.INPUT,
      parentDisposable = parentDisposable,
      textProperty = builtInServerMapping,
      initialLanguage = JsonLanguage.INSTANCE,
      minimumSizeHeight = 220,
    )
  private val outputEditor =
    AdvancedEditor(
      id = "httpServerOutput",
      context = context,
      configuration = configuration,
      project = project,
      title = null,
      editorMode = AdvancedEditor.EditorMode.OUTPUT,
      parentDisposable = parentDisposable,
    )

  private val processLock = Any()

  @Volatile private var wireMockProcess: KillableProcessHandler? = null
  private var startedServerMode: ServerMode? = null
  private var startedServerPort: Int? = null
  private var startedVerboseLogging: Boolean? = null
  private var startedPrintAllNetworkTraffic: Boolean? = null
  private var startedAdvancedCommandLineOptions: String? = null
  private var startedJavaExecutableMode: JavaExecutableMode? = null
  private var startedJavaExecutablePath: String? = null

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  init {
    serverPort.afterChange(parentDisposable) { updateRestartWarning() }
    verboseLogging.afterChange(parentDisposable) { updateRestartWarning() }
    printAllNetworkTraffic.afterChange(parentDisposable) { updateRestartWarning() }
    advancedCommandLineOptions.afterChange(parentDisposable) { updateRestartWarning() }
    javaExecutableMode.afterChange(parentDisposable) { updateRestartWarning() }
    javaExecutablePath.afterChange(parentDisposable) { updateRestartWarning() }
    serverMode.afterChange(parentDisposable) { updateRestartWarning() }
    serverRunning.afterChange(parentDisposable) { updateRestartWarning() }

    builtInServerMapping.afterChange(parentDisposable) { syncBuiltInServerMappingsIfNeeded() }
  }

  override fun Panel.buildUi() {
    row { cell(content).resizableColumn().align(Align.FILL) }.resizableRow()
  }

  override fun afterBuildUi() {
    syncContent()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun syncContent() {
    setContent(if (isWireMockDownloaded()) createConfigurationUi() else createDownloadUi())
  }

  private fun setContent(component: JComponent) {
    runOnEdt {
      content.removeAll()
      content.addToCenter(component)
      content.revalidate()
      content.repaint()
    }
  }

  private fun createDownloadUi(): JComponent = panel {
    row { cell(createDownloadDescription()).resizableColumn().align(Align.FILL) }

    row { cell(createMavenCentralLink()) }.topGap(TopGap.NONE)

    row {
      lateinit var downloadButton: JButton
      downloadButton =
        button(UiToolsBundle.message("http-server.download.button")) {
            downloadWireMockStandalone(downloadButton)
          }
          .component
    }
  }

  private fun createDownloadDescription(): JComponent =
    JBLabel("<html>${UiToolsBundle.message("http-server.download.description")}</html>")

  private fun createMavenCentralLink(): JComponent =
    HyperlinkLabel(UiToolsBundle.message("http-server.download.maven-central-link")).apply {
      setHyperlinkTarget(MAVEN_CENTRAL_HTTP_REPOSITORY_URL)
    }

  private fun createConfigurationUi(): JComponent = panel {
    row {
      label("")
        .label(UiToolsBundle.message("http-server.server.status.label"))
        .bindText(serverStatus)

      button(UiToolsBundle.message("http-server.server.start")) { startWireMock() }
        .visibleIf(PropertyComponentPredicate(serverRunning, false))
        .enabledIf(PropertyComponentPredicate(serverBusy, false))
        .gap(RightGap.SMALL)

      button(UiToolsBundle.message("http-server.server.restart")) { restartWireMock() }
        .visibleIf(PropertyComponentPredicate(serverRunning, true))
        .enabledIf(PropertyComponentPredicate(serverBusy, false))
        .gap(RightGap.SMALL)

      button(UiToolsBundle.message("http-server.server.stop")) { stopWireMock() }
        .visibleIf(PropertyComponentPredicate(serverRunning, true))
        .enabledIf(PropertyComponentPredicate(serverBusy, false))
        .gap(RightGap.SMALL)

      contextHelp(UiToolsBundle.message("http-server.server.auto-reload-info"))
    }

    row { cell(createServerUrlLink()) }.visibleIf(PropertyComponentPredicate(serverRunning, true))

    row {
        icon(AllIcons.General.Warning).gap(RightGap.SMALL)
        label(UiToolsBundle.message("http-server.server.restart-required"))
      }
      .visibleIf(PropertyComponentPredicate(restartRequired, true))

    row {
        cell(
            JBTabbedPane().apply {
              applyDefaultTabComponentInsets()

              addTab(
                UiToolsBundle.message("http-server.tab.configuration"),
                createConfigurationTab(),
              )
              addTab(UiToolsBundle.message("http-server.tab.output"), createOutputTab())
            }
          )
          .resizableColumn()
          .align(Align.FILL)
      }
      .resizableRow()
  }

  private fun createConfigurationTab(): JComponent = panel {
    row {
      textField()
        .label(UiToolsBundle.message("http-server.server.port.label"))
        .bindIntTextImproved(serverPort)
        .validateLongValue(LongRange(1, 65_535))
        .columns(6)
    }

    row {
      checkBox(UiToolsBundle.message("http-server.server.verbose-logging"))
        .bindSelected(verboseLogging)
    }

    row {
      checkBox(UiToolsBundle.message("http-server.server.print-all-network-traffic"))
        .bindSelected(printAllNetworkTraffic)
    }

    row { cell(createAdvancedConfigLink()) }.topGap(TopGap.NONE)

    buttonsGroup {
      row {
        val customDirectoryRadioButton =
          radioButton(UiToolsBundle.message("http-server.configuration.mode.custom-directory"))
            .bind(serverMode, ServerMode.CUSTOM_DIRECTORY)
            .gap(RightGap.SMALL)
        textFieldWithBrowseButton(
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
              .withTitle(UiToolsBundle.message("http-server.configuration.custom-directory.title")),
            project,
          )
          .bindText(customRootDirectory)
          .enabledIf(customDirectoryRadioButton.selected)
          .resizableColumn()
          .align(Align.FILL)
          .gap(RightGap.SMALL)
      }

      row {
        radioButton(UiToolsBundle.message("http-server.configuration.mode.built-in-server"))
          .bind(serverMode, ServerMode.BUILT_IN_SERVER)
      }
    }

    row { cell(builtInServerMappingEditor.component).resizableColumn().align(Align.FILL) }
      .resizableRow()
      .topGap(TopGap.SMALL)
      .visibleIf(PropertyComponentPredicate(serverMode, ServerMode.BUILT_IN_SERVER))

    row { cell(createBuiltInServerDirectoryLink()) }
      .topGap(TopGap.NONE)
      .bottomGap(BottomGap.NONE)
      .visibleIf(PropertyComponentPredicate(serverMode, ServerMode.BUILT_IN_SERVER))

    row { cell(createMappingsDocumentationLink()) }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
  }

  private fun createOutputTab(): JComponent = panel {
    row { cell(outputEditor.component).resizableColumn().align(Align.FILL) }
      .layout(RowLayout.INDEPENDENT)
      .resizableRow()

    row { cell(createClearOutputLink()) }
  }

  private fun createClearOutputLink(): JComponent =
    HyperlinkLabel(UiToolsBundle.message("http-server.output.clear")).apply {
      addHyperlinkListener { event ->
        if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
          clearProcessOutput()
        }
      }
    }

  private fun createMappingsDocumentationLink(): JComponent =
    HyperlinkLabel().apply {
      setTextWithHyperlink(
        UiToolsBundle.message("http-server.configuration.mappings.documentation")
      )
      setHyperlinkTarget(WIREMOCK_MAPPINGS_DOCUMENTATION_URL)
    }

  private fun createCommandLineOptionsDocumentationLink(): JComponent =
    HyperlinkLabel().apply {
      setTextWithHyperlink(UiToolsBundle.message("http-server.advanced-config.documentation"))
      setHyperlinkTarget(WIREMOCK_COMMAND_LINE_OPTIONS_DOCUMENTATION_URL)
    }

  private fun createAdvancedConfigLink(): JComponent =
    HyperlinkLabel(UiToolsBundle.message("http-server.server.advanced-config")).apply {
      addHyperlinkListener { event ->
        if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
          showAdvancedConfigDialog()
        }
      }
    }

  private fun createBuiltInServerDirectoryLink(): JComponent =
    HyperlinkLabel(UiToolsBundle.message("http-server.configuration.built-in-server.directory"))
      .apply {
        addHyperlinkListener { event ->
          if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
            Files.createDirectories(builtInServerFilesDirectory)
            BrowserUtil.browse(builtInServerRootDirectory)
          }
        }
      }

  private fun createServerUrlLink(): JComponent =
    HyperlinkLabel().apply {
      updateServerUrlLink(serverUrl.get())
      addHyperlinkListener { event ->
        if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
          BrowserUtil.open(serverUrl.get())
        }
      }
      serverUrl.afterChange(parentDisposable) { updateServerUrlLink(it) }
    }

  private fun HyperlinkLabel.updateServerUrlLink(url: String) {
    setTextWithHyperlink("<hyperlink>$url</hyperlink>")
    setHyperlinkTarget(url)
  }

  private fun downloadWireMockStandalone(downloadButton: JButton) {
    object :
        Task.Backgroundable(project, UiToolsBundle.message("http-server.download.task-title")) {

        override fun run(indicator: ProgressIndicator) {
          setButtonEnabled(downloadButton, false)
          indicator.isIndeterminate = true
          indicator.text = UiToolsBundle.message("http-server.download.task-title")

          downloadWireMockStandaloneJar()
        }

        override fun onSuccess() {
          syncContent()
        }

        override fun onThrowable(error: Throwable) {
          log.warn("Failed to download WireMock standalone to: $wireMockStandaloneJarPath", error)

          Messages.showErrorDialog(
            project,
            UiToolsBundle.message(
              "http-server.download.failed",
              "${error::class.simpleName ?: error::class.java.simpleName}: ${error.message ?: "Unknown error"}",
            ),
            UiToolsBundle.message("http-server.download.failed-title"),
          )
        }

        override fun onFinished() {
          if (!isWireMockDownloaded()) {
            setButtonEnabled(downloadButton, true)
          }
        }
      }
      .queue()
  }

  private fun downloadWireMockStandaloneJar() {
    val httpClient =
      OkHttpClient.Builder().applyIntelliJProxySettings(WIREMOCK_STANDALONE_DOWNLOAD_URL).build()
    val request = Request.Builder().get().url(WIREMOCK_STANDALONE_DOWNLOAD_URL).build()

    Files.createDirectories(wireMockStandaloneJarPath.parent)
    val temporaryDownloadPath =
      wireMockStandaloneJarPath.resolveSibling("${wireMockStandaloneJarPath.fileName}.part")

    try {
      Files.deleteIfExists(temporaryDownloadPath)

      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          throw IllegalStateException(
            "Failed to download WireMock standalone from $WIREMOCK_STANDALONE_DOWNLOAD_URL: HTTP ${response.code}"
          )
        }

        response.body.byteStream().use { inputStream ->
          Files.copy(inputStream, temporaryDownloadPath, StandardCopyOption.REPLACE_EXISTING)
        }
      }

      Files.move(
        temporaryDownloadPath,
        wireMockStandaloneJarPath,
        StandardCopyOption.REPLACE_EXISTING,
      )
    } catch (e: Throwable) {
      try {
        Files.deleteIfExists(temporaryDownloadPath)
      } catch (cleanupError: Exception) {
        log.warn(
          "Failed to delete temporary WireMock download: $temporaryDownloadPath",
          cleanupError,
        )
      }

      throw e
    }
  }

  private fun isWireMockDownloaded(): Boolean =
    Files.isRegularFile(wireMockStandaloneJarPath) &&
      runCatching { Files.size(wireMockStandaloneJarPath) > 0L }.getOrDefault(false)

  private fun syncBuiltInServerMappingsIfNeeded() {
    if (serverMode.get() != ServerMode.BUILT_IN_SERVER || !isWireMockDownloaded()) {
      return
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      runCatching { prepareBuiltInServerRootDirectory() }
        .onFailure { error -> log.warn("Failed to update generated WireMock mappings", error) }
    }
  }

  private fun startWireMock() {
    if (serverBusy.get()) {
      return
    }

    appendProcessEvent("Starting WireMock")
    startOrRestartWireMock(
      busyStatus = UiToolsBundle.message("http-server.server.status.starting"),
      failureLogMessage = "Failed to start WireMock",
      failureTitle = UiToolsBundle.message("http-server.server.start.failed-title"),
      createFailureMessage = { errorMessage ->
        UiToolsBundle.message("http-server.server.start.failed", errorMessage)
      },
    )
  }

  private fun restartWireMock() {
    if (serverBusy.get()) {
      return
    }

    val process = currentWireMockProcess()
    if (process == null) {
      startWireMock()
      return
    }

    appendProcessEvent("Restarting WireMock")
    startOrRestartWireMock(
      processToStop = process,
      busyStatus = UiToolsBundle.message("http-server.server.status.restarting"),
      failureLogMessage = "Failed to restart WireMock",
      failureTitle = UiToolsBundle.message("http-server.server.restart.failed-title"),
      createFailureMessage = { errorMessage ->
        UiToolsBundle.message("http-server.server.restart.failed", errorMessage)
      },
    )
  }

  private fun stopWireMock() {
    if (serverBusy.get()) {
      return
    }

    val process = currentWireMockProcess() ?: return

    setServerBusy(true, UiToolsBundle.message("http-server.server.status.stopping"))
    appendProcessEvent("Stopping WireMock")

    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        stopWireMockProcess(process)
        appendProcessEvent("WireMock stopped")
        setServerRunning(false, UiToolsBundle.message("http-server.server.status.stopped"))
      } catch (e: Exception) {
        log.warn("Failed to stop WireMock", e)
        appendProcessEvent("Failed to stop WireMock: ${e.message ?: "Unknown error"}")
        showWireMockErrorDialog(
          UiToolsBundle.message("http-server.server.stop.failed-title"),
          UiToolsBundle.message("http-server.server.stop.failed", e.message ?: "Unknown error"),
        )
      } finally {
        setServerBusy(false)
      }
    }
  }

  private fun startOrRestartWireMock(
    processToStop: KillableProcessHandler? = null,
    busyStatus: String,
    failureLogMessage: String,
    failureTitle: String,
    createFailureMessage: (String) -> String,
  ) {
    setServerBusy(true, busyStatus)

    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        processToStop?.let { stopWireMockProcess(it) }

        val rootDirectory = prepareRootDirectoryForCurrentConfiguration()
        startWireMockProcess(rootDirectory)
        startedServerMode = serverMode.get()
        startedServerPort = serverPort.get()
        startedVerboseLogging = verboseLogging.get()
        startedPrintAllNetworkTraffic = printAllNetworkTraffic.get()
        startedAdvancedCommandLineOptions = advancedCommandLineOptions.get()
        startedJavaExecutableMode = javaExecutableMode.get()
        startedJavaExecutablePath = javaExecutablePath.get()

        appendProcessEvent("WireMock running at ${currentServerUrl()}")
        setServerRunning(
          true,
          UiToolsBundle.message("http-server.server.status.running"),
          currentServerUrl(),
        )
      } catch (e: Exception) {
        log.warn(failureLogMessage, e)
        appendProcessEvent(failureSummaryMessage(e))
        setServerRunning(false, UiToolsBundle.message("http-server.server.status.stopped"))
        showWireMockErrorDialog(failureTitle, createFailureMessage(failureDetailedMessage(e)))
      } finally {
        setServerBusy(false)
      }
    }
  }

  private fun prepareRootDirectoryForCurrentConfiguration(): Path =
    when (serverMode.get()) {
      ServerMode.CUSTOM_DIRECTORY -> resolveCustomRootDirectory()
      ServerMode.BUILT_IN_SERVER -> prepareBuiltInServerRootDirectory()
    }

  private fun resolveCustomRootDirectory(): Path {
    val customRootDirectory = customRootDirectory.get().trim()
    check(customRootDirectory.isNotEmpty()) {
      UiToolsBundle.message("http-server.configuration.custom-directory.missing")
    }

    val rootDirectory =
      try {
        Paths.get(customRootDirectory)
      } catch (_: InvalidPathException) {
        throw IllegalStateException(
          UiToolsBundle.message("http-server.configuration.custom-directory.invalid")
        )
      }

    if (Files.exists(rootDirectory) && !Files.isDirectory(rootDirectory)) {
      throw IllegalStateException(
        UiToolsBundle.message("http-server.configuration.custom-directory.invalid")
      )
    }

    Files.createDirectories(rootDirectory)

    return rootDirectory
  }

  private fun prepareBuiltInServerRootDirectory(): Path {
    Files.createDirectories(builtInServerMappingsDirectory)
    deleteGeneratedBuiltInServerMappings()
    writeJson(
      builtInServerMappingsDirectory.resolve("built-in-server-mapping.json"),
      builtInServerMapping.get(),
    )

    return builtInServerRootDirectory
  }

  private fun deleteGeneratedBuiltInServerMappings() {
    Files.list(builtInServerMappingsDirectory).use { mappingFiles ->
      mappingFiles.filter { Files.isRegularFile(it) }.forEach { Files.deleteIfExists(it) }
    }
  }

  private fun writeJson(file: Path, content: Any) {
    Files.createDirectories(file.parent)
    ObjectMapperService.instance
      .jsonMapper()
      .writerWithDefaultPrettyPrinter()
      .writeValue(
        file.toFile(),
        if (content is String) {
          ObjectMapperService.instance.jsonMapper().readTree(content)
        } else {
          content
        },
      )
  }

  private fun startWireMockProcess(rootDirectory: Path): KillableProcessHandler {
    check(isWireMockDownloaded()) {
      UiToolsBundle.message("http-server.download.missing-start-blocked")
    }

    val javaExecutable = currentJavaExecutable()
    val parameters = buildList {
      add("-jar")
      add(wireMockStandaloneJarPath.toString())
      add("--port")
      add(serverPort.get().toString())
      add("--root-dir")
      add(rootDirectory.toAbsolutePath().normalize().toString())
      if (serverMode.get() == ServerMode.BUILT_IN_SERVER) {
        add("--local-response-templating")
      }
      if (verboseLogging.get()) {
        add("--verbose")
      }
      if (printAllNetworkTraffic.get()) {
        add("--print-all-network-traffic")
      }
      addAll(parseAdvancedCommandLineOptions())
    }

    val commandLine =
      GeneralCommandLine()
        .withExePath(javaExecutable.toString())
        .withParameters(parameters)
        .withRedirectErrorStream(true)

    appendProcessEvent("Command: ${commandLine.commandLineString}")

    val processOutput = StringBuilder()
    val processHandler = KillableProcessHandler(commandLine)
    consumeProcessOutput(processHandler, processOutput)
    watchWireMockProcess(processHandler)
    synchronized(processLock) { wireMockProcess = processHandler }
    registerWireMockProcess(processHandler)
    processHandler.startNotify()

    if (processHandler.waitFor(STARTUP_TIMEOUT_MILLISECONDS)) {
      val exitCode = processHandler.exitCode ?: -1
      unregisterWireMockProcess(processHandler)
      synchronized(processLock) {
        if (wireMockProcess === processHandler) {
          wireMockProcess = null
        }
      }
      throw IllegalStateException(
        UiToolsBundle.message(
          "http-server.server.start.process-exited",
          exitCode,
          buildProcessOutputMessage(processOutput),
        )
      )
    }

    return processHandler
  }

  private fun consumeProcessOutput(
    processHandler: KillableProcessHandler,
    processOutput: StringBuilder,
  ) {
    processHandler.addProcessListener(
      object : ProcessAdapter() {

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
          val outputChunk = event.text
          synchronized(processOutput) {
            processOutput.append(outputChunk)
            if (processOutput.length > MAX_STARTUP_PROCESS_OUTPUT_LENGTH) {
              processOutput.delete(0, processOutput.length - MAX_STARTUP_PROCESS_OUTPUT_LENGTH)
            }
          }
          outputEditor.appendText(outputChunk)
        }
      }
    )
  }

  private fun watchWireMockProcess(processHandler: KillableProcessHandler) {
    processHandler.addProcessListener(
      object : ProcessAdapter() {

        override fun processTerminated(event: ProcessEvent) {
          unregisterWireMockProcess(processHandler)
          val wasCurrentProcess =
            synchronized(processLock) {
              if (wireMockProcess === processHandler) {
                wireMockProcess = null
                true
              } else {
                false
              }
            }

          if (wasCurrentProcess && !serverBusy.get() && !isDisposed) {
            appendProcessEvent("WireMock exited with code ${event.exitCode}")
            setServerRunning(
              false,
              UiToolsBundle.message("http-server.server.status.stopped.exit-code", event.exitCode),
            )
          }
        }
      }
    )
  }

  private fun stopWireMockProcess(process: KillableProcessHandler) {
    synchronized(processLock) {
      if (wireMockProcess === process) {
        wireMockProcess = null
      }
    }
    unregisterWireMockProcess(process)

    process.destroyProcess()
    if (!process.waitFor(STOP_TIMEOUT_MILLISECONDS)) {
      if (process.canKillProcess()) {
        process.killProcess()
      }
      if (!process.waitFor(STOP_TIMEOUT_MILLISECONDS)) {
        throw IllegalStateException(UiToolsBundle.message("http-server.server.stop.timeout"))
      }
    }
  }

  private fun currentWireMockProcess(): KillableProcessHandler? =
    synchronized(processLock) {
      val process = wireMockProcess
      if (process != null && process.isProcessTerminated) {
        wireMockProcess = null
        null
      } else {
        process
      }
    }

  private fun registerWireMockProcess(process: KillableProcessHandler) {
    project?.service<HttpServerProjectProcessShutdownService>()
    ApplicationManager.getApplication()
      .service<ExternalSystemProcessRegistry>()
      .register(project, process)
  }

  private fun unregisterWireMockProcess(process: KillableProcessHandler) {
    ApplicationManager.getApplication().service<ExternalSystemProcessRegistry>().unregister(process)
  }

  private fun currentJavaExecutable(): Path {
    if (javaExecutableMode.get() == JavaExecutableMode.PATH) {
      val customJavaExecutable = javaExecutablePath.get().trim()
      check(customJavaExecutable.isNotEmpty()) {
        UiToolsBundle.message("http-server.advanced-config.java-executable.path.missing")
      }

      return try {
        Paths.get(customJavaExecutable)
      } catch (_: InvalidPathException) {
        throw IllegalStateException(
          UiToolsBundle.message("http-server.advanced-config.java-executable.path.invalid")
        )
      }
    }

    val javaExecutableFileName =
      if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"

    return Paths.get(System.getProperty("java.home"), "bin", javaExecutableFileName)
  }

  private fun buildProcessOutputMessage(processOutput: StringBuilder): String =
    synchronized(processOutput) {
      processOutput.toString().trim().ifBlank {
        UiToolsBundle.message("http-server.server.process-output.empty")
      }
    }

  private fun failureSummaryMessage(error: Throwable): String {
    val detailedMessage = failureDetailedMessage(error)
    val summaryMessage =
      detailedMessage.substringBefore("\n").let { firstLine ->
        if (". " in firstLine) {
          firstLine.substringBefore(". ") + "."
        } else {
          firstLine
        }
      }

    return summaryMessage.ifBlank { "Unknown error" }
  }

  private fun failureDetailedMessage(error: Throwable): String = error.message ?: "Unknown error"

  private fun clearProcessOutput() {
    outputEditor.clearText()
  }

  private fun appendProcessEvent(message: String) {
    val timestamp = LocalTime.now().format(processEventTimestampFormat)
    outputEditor.appendText("[$timestamp] $message\n")
  }

  private fun setServerBusy(busy: Boolean, status: String? = null) {
    runOnEdt {
      serverBusy.set(busy)
      status?.let { serverStatus.set(it) }
    }
  }

  private fun setServerRunning(running: Boolean, status: String, url: String = "") {
    runOnEdt {
      serverRunning.set(running)
      serverStatus.set(status)
      serverUrl.set(url)
    }
  }

  private fun updateRestartWarning() {
    val restartRequired =
      (serverRunning.get() &&
        (startedServerPort != serverPort.get() ||
          startedServerMode != serverMode.get() ||
          startedVerboseLogging != verboseLogging.get() ||
          startedPrintAllNetworkTraffic != printAllNetworkTraffic.get() ||
          startedAdvancedCommandLineOptions != advancedCommandLineOptions.get() ||
          startedJavaExecutableMode != javaExecutableMode.get() ||
          (javaExecutableMode.get() == JavaExecutableMode.PATH &&
            startedJavaExecutablePath != javaExecutablePath.get())))

    runOnEdt { this.restartRequired.set(restartRequired) }
  }

  private fun showAdvancedConfigDialog() {
    val commandLineOptions = ValueProperty(advancedCommandLineOptions.get())
    val selectedJavaExecutableMode = ValueProperty(javaExecutableMode.get())
    val selectedJavaExecutablePath = ValueProperty(javaExecutablePath.get())
    val commandLineOptionsEditor =
      AdvancedEditor(
          id = "httpServerAdvancedCommandLineOptions",
          context = context,
          configuration = configuration,
          project = project,
          title = null,
          editorMode = AdvancedEditor.EditorMode.INPUT,
          parentDisposable = parentDisposable,
          textProperty = commandLineOptions,
          minimumSizeHeight = 220,
        )
        .apply { component.preferredSize = Dimension(700, 260) }

    val apply =
      object : DialogWrapper(project, content, true, IdeModalityType.IDE) {

          init {
            title = UiToolsBundle.message("http-server.advanced-config.title")
            init()
          }

          override fun createCenterPanel(): JComponent = panel {
            row { label(UiToolsBundle.message("http-server.advanced-config.command-line-options")) }

            row { cell(commandLineOptionsEditor.component).align(Align.FILL).resizableColumn() }
              .resizableRow()

            row { cell(createCommandLineOptionsDocumentationLink()) }
              .topGap(TopGap.SMALL)
              .bottomGap(BottomGap.NONE)

            buttonsGroup(UiToolsBundle.message("http-server.advanced-config.java-executable")) {
              row {
                radioButton(
                    UiToolsBundle.message(
                      "http-server.advanced-config.java-executable.built-in-jre"
                    )
                  )
                  .bind(selectedJavaExecutableMode, JavaExecutableMode.BUILT_IN_JRE)
              }

              row {
                radioButton(
                    UiToolsBundle.message("http-server.advanced-config.java-executable.path")
                  )
                  .bind(selectedJavaExecutableMode, JavaExecutableMode.PATH)
                  .gap(RightGap.SMALL)
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileDescriptor()
                      .withTitle(
                        UiToolsBundle.message(
                          "http-server.advanced-config.java-executable.path.title"
                        )
                      ),
                    project,
                  )
                  .bindText(selectedJavaExecutablePath)
                  .enabledIf(
                    PropertyComponentPredicate(selectedJavaExecutableMode, JavaExecutableMode.PATH)
                  )
                  .resizableColumn()
                  .align(Align.FILL)
              }
            }
          }

          override fun getDimensionServiceKey(): String =
            "${HttpServer::class.java.name}.AdvancedConfigDialog"
        }
        .showAndGet()

    if (apply) {
      advancedCommandLineOptions.set(commandLineOptions.get())
      javaExecutableMode.set(selectedJavaExecutableMode.get())
      javaExecutablePath.set(selectedJavaExecutablePath.get())
    }
  }

  private fun parseAdvancedCommandLineOptions(): List<String> =
    advancedCommandLineOptions
      .get()
      .lineSequence()
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .toList()

  private fun currentServerUrl(): String = "http://localhost:${serverPort.get()}"

  private fun showWireMockErrorDialog(title: String, message: String) {
    runOnEdt { Messages.showErrorDialog(project, message, title) }
  }

  private fun setButtonEnabled(downloadButton: JButton, enabled: Boolean) {
    runOnEdt { downloadButton.isEnabled = enabled }
  }

  private fun runOnEdt(action: () -> Unit) {
    if (ApplicationManager.getApplication().isDispatchThread) {
      action()
    } else {
      ApplicationManager.getApplication().invokeLater(action)
    }
  }

  override fun doDispose() {
    currentWireMockProcess()?.let { process ->
      runCatching { stopWireMockProcess(process) }
        .onFailure { error -> log.warn("Failed to dispose running WireMock process", error) }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class ServerMode {

    CUSTOM_DIRECTORY,
    BUILT_IN_SERVER,
  }

  enum class JavaExecutableMode {

    BUILT_IN_JRE,
    PATH,
  }

  class Factory : DeveloperUiToolFactory<HttpServer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("http-server.menu-title"),
        contentTitle = UiToolsBundle.message("http-server.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> HttpServer) = { configuration ->
      HttpServer(
        configuration = configuration,
        project = project,
        context = context,
        parentDisposable = parentDisposable,
      )
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val WIREMOCK_STANDALONE_VERSION = "3.13.2"
    private const val MAVEN_CENTRAL_HTTP_REPOSITORY_URL =
      "https://mvnrepository.com/artifact/org.wiremock/wiremock-standalone/$WIREMOCK_STANDALONE_VERSION"
    private const val WIREMOCK_MAPPINGS_DOCUMENTATION_URL = "https://wiremock.org/docs/stubbing/"
    private const val WIREMOCK_COMMAND_LINE_OPTIONS_DOCUMENTATION_URL =
      "https://wiremock.org/docs/standalone/java-jar/#command-line-options"
    private const val WIREMOCK_STANDALONE_DOWNLOAD_URL =
      "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/$WIREMOCK_STANDALONE_VERSION/wiremock-standalone-$WIREMOCK_STANDALONE_VERSION.jar"
    private const val DEFAULT_WIREMOCK_PORT = 8089
    private const val STARTUP_TIMEOUT_MILLISECONDS = 1_500L
    private const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    private const val MAX_STARTUP_PROCESS_OUTPUT_LENGTH = 8_000
    private const val DEFAULT_ADVANCED_COMMAND_LINE_OPTIONS = "--disable-banner"
    private val processEventTimestampFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
    internal val httpServerToolRootPath: Path =
      PathManager.getSystemDir().resolve(Paths.get("plugins", "developer-tools", "http-server"))
    internal val wireMockStandaloneJarPath: Path =
      httpServerToolRootPath.resolve("wiremock-standalone-$WIREMOCK_STANDALONE_VERSION.jar")
    private val builtInServerRootDirectory = httpServerToolRootPath.resolve("built-in-server")
    private val builtInServerMappingsDirectory = builtInServerRootDirectory.resolve("mappings")
    private val builtInServerFilesDirectory = builtInServerRootDirectory.resolve("__files")
  }
}
