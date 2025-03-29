package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import ai.grazie.utils.capitalize
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.OkHttpClientUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.common.OkHttpClientUtils.applyIntelliJProxySettings
import dev.turingcomplete.intellijdevelopertoolsplugin.common.safeCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor.EditorMode.OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AnActionOptionButton
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils.createPopup
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.copyable
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.awt.datatransfer.StringSelection
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Date
import java.util.StringJoiner
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent
import okhttp3.OkHttpClient
import okhttp3.Request

class ServerCertificates(
  private val project: Project?,
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
) : DeveloperUiTool(parentDisposable), DataProvider {
  // -- Properties ---------------------------------------------------------- //

  private val log = logger<ServerCertificates>()

  private val url = configuration.register("url", "", INPUT, "https://jetbrains.com")
  private val followRedirects = configuration.register("followRedirects", true, CONFIGURATION)
  private val allowInsecureConnection =
    configuration.register("allowInsecureConnection", false, CONFIGURATION)
  private val certificatesPanel = BorderLayoutPanel()

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
        expandableTextField()
          .label(UiToolsBundle.message("server-certificates.url"))
          .bindText(url)
          .resizableColumn()
          .align(Align.FILL)
      }
      .bottomGap(BottomGap.NONE)
    row {
        checkBox(UiToolsBundle.message("server-certificates.follow-redirects"))
          .bindSelected(followRedirects)
      }
      .topGap(TopGap.NONE)
    row {
        checkBox(UiToolsBundle.message("server-certificates.allow-insecure-connection"))
          .bindSelected(allowInsecureConnection)
          .gap(RightGap.SMALL)
        contextHelp(UiToolsBundle.message("server-certificates.allow-insecure-connection-help"))
      }
      .topGap(TopGap.NONE)

    row {
      button(UiToolsBundle.message("server-certificates.fetch-server-certificates")) {
        val url = url.get()
        fetchCertificates(
          project = project,
          url = url,
          allowInsecureConnection = allowInsecureConnection.get(),
          onStarted = { setCertificatesResultUi(createFetchingUi()) },
          onSuccess = { setCertificatesResultUi(createCertificatesUi(it)) },
          onCancel = { setCertificatesResultUi(null) },
          onThrowable = { e ->
            log.warn("Failed to retrieve server certificates from: $url", e)
            setCertificatesResultUi(createFetchingFailedUi(e))
          },
        )
      }
    }

    row { cell(certificatesPanel).resizableColumn().align(Align.FILL) }
      .resizableRow()
      .topGap(TopGap.MEDIUM)
  }

  private fun setCertificatesResultUi(component: JComponent?) {
    ApplicationManager.getApplication().invokeLater {
      certificatesPanel.removeAll()
      if (component != null) {
        certificatesPanel.addToCenter(component)
      }
      certificatesPanel.revalidate()
      certificatesPanel.repaint()
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createFetchingUi(): JComponent = panel {
    row {
      label(UiToolsBundle.message("server-certificates.fetch-server-certificates-in-progress"))
        .align(Align.FILL)
        .resizableColumn()
    }
  }

  private fun createFetchingFailedUi(e: Throwable): JComponent = panel {
    row {
      icon(AllIcons.General.BalloonError).gap(RightGap.SMALL)
      label(
          UiToolsBundle.message(
            "server-certificates.fetch-server-certificates-failed",
            "${e::class.simpleName}: ${e.message}",
          )
        )
        .align(Align.FILL)
        .resizableColumn()
    }
  }

  private fun createCertificatesUi(httpResponse: HttpResponse): JComponent = panel {
    group(UiToolsBundle.message("server-certificates.result"), false) {
      row {
        cell(
          HyperlinkLabel(
              UiToolsBundle.message(
                "server-certificates.response",
                httpResponse.statusCode,
                httpResponse.statusMessage,
              )
            )
            .apply {
              addHyperlinkListener(createShowHttpResponseHyperlinkHandler(httpResponse, this))
            }
        )
      }

      if (httpResponse.certificates?.isNotEmpty() == true) {
        buildCertificatesExportUi(httpResponse.certificates)

        httpResponse.certificates.forEachIndexed { index, certificate ->
          group(UiToolsBundle.message("server-certificates.certificate-title", index + 1), false) {
            if (certificate is X509Certificate) {
              buildCertificatePropertiesUi(certificate)
              buildCertificateValidityUi(certificate)
            }
            buildCertificatesExportUi(listOf(certificate))
          }
        }
      } else {
        row { label("<html>${UiToolsBundle.message("server-certificates.no-result")}</html>") }
      }
    }
  }

  private fun createShowHttpResponseHyperlinkHandler(
    httpResponse: HttpResponse,
    parentComponent: JComponent,
  ): HyperlinkAdapter =
    object : HyperlinkAdapter() {
      override fun hyperlinkActivated(e: HyperlinkEvent) {
        val content = panel {
          row {
              cell(
                  AdvancedEditor(
                      id = "server-certificates-http-response",
                      context = context,
                      configuration = configuration,
                      project = project,
                      title = null,
                      editorMode = OUTPUT,
                      parentDisposable = parentDisposable,
                    )
                    .apply {
                      text =
                        with(StringJoiner(System.lineSeparator())) {
                          add(
                            "${httpResponse.protocol} ${httpResponse.statusCode} ${httpResponse.statusMessage}"
                          )
                          httpResponse.headers.forEach {
                            add("${it.key}: ${it.value.joinToString(", ") { it ?: "" }}")
                          }
                          httpResponse.body?.let {
                            add("")
                            add(it)
                          }
                          toString()
                        }
                    }
                    .component
                )
                .resizableColumn()
                .align(Align.FILL)
            }
            .resizableRow()
        }
        createPopup(content).showInCenterOf(parentComponent)
      }
    }

  private fun Panel.buildCertificatesExportUi(certificates: List<Certificate>) {
    row {
      lateinit var exportActionsButton: JComponent
      exportActionsButton =
        AnActionOptionButton(
          ShowAsPemAction(certificates, context, configuration, project, parentDisposable) {
            exportActionsButton
          },
          ExportAsPemAction(certificates, url.get()),
          ExportAsDerAction(certificates, url.get()),
          ExportAsJksAction(certificates, url.get()),
          CopyAsPemToClipboardAction(certificates),
          ShowCertificateDetailsAction(
            certificates,
            context,
            configuration,
            project,
            parentDisposable,
          ) {
            exportActionsButton
          },
        )
      cell(exportActionsButton)
    }
  }

  private fun Panel.buildCertificatePropertiesUi(certificate: X509Certificate) {
    listOf<Pair<String, Any>>(
        UiToolsBundle.message("server-certificates.certificate-subject") to
          certificate.subjectX500Principal,
        UiToolsBundle.message("server-certificates.certificate-issuer") to
          certificate.issuerX500Principal,
        UiToolsBundle.message("server-certificates.certificate-serial-number") to
          certificate.serialNumber,
        UiToolsBundle.message("server-certificates.certificate-valid-from") to
          certificate.notBefore,
        UiToolsBundle.message("server-certificates.certificate-valid-to") to certificate.notAfter,
        UiToolsBundle.message("server-certificates.certificate-signature-algorithm") to
          certificate.sigAlgName,
      )
      .forEach { (title, value) ->
        row("$title:") {
          val stringValue =
            when (value) {
              is String -> value
              is BigInteger -> value.toString(16).uppercase()
              is X500Principal -> value.toString()
              is Date -> {
                val diff = DateFormatUtil.formatBetweenDates(value.time, System.currentTimeMillis())
                "${DateFormatUtil.formatDateTime(value)} (${diff.capitalize()})"
              }

              else -> throw IllegalStateException("Unknown property type: ${value::class}")
            }
          cell(JBLabel(stringValue).copyable()).gap(RightGap.SMALL)
        }
      }
  }

  private fun Panel.buildCertificateValidityUi(certificate: X509Certificate) {
    row {
      try {
        certificate.checkValidity()
      } catch (_: CertificateExpiredException) {
        icon(AllIcons.General.Warning).gap(RightGap.SMALL)
        label(UiToolsBundle.message("server-certificates.certificate-expired"))
      } catch (_: CertificateNotYetValidException) {
        icon(AllIcons.General.Warning).gap(RightGap.SMALL)
        label(UiToolsBundle.message("server-certificates.certificate-not-valid-yet"))
      }
    }
  }

  private fun fetchCertificates(
    project: Project?,
    url: String,
    allowInsecureConnection: Boolean,
    onStarted: () -> Unit,
    onSuccess: (HttpResponse) -> Unit,
    onCancel: () -> Unit,
    onThrowable: (Throwable) -> Unit,
  ) {
    object :
        Task.Backgroundable(
          project,
          UiToolsBundle.message("server-certificates.fetch-server-certificates-in-progress-title"),
          true,
        ) {
        override fun run(indicator: ProgressIndicator) {
          indicator.text =
            UiToolsBundle.message("server-certificates.fetch-server-certificates-in-progress")
          onStarted()

          val httpClientBuilder =
            OkHttpClient.Builder()
              .followRedirects(followRedirects.get())
              .followSslRedirects(followRedirects.get())
              .applyIntelliJProxySettings(url)

          val certificateCapturingTrustManager =
            CertificateCapturingTrustManager(allowInsecureConnection)
          httpClientBuilder.sslSocketFactory(
            certificateCapturingTrustManager.createSslContext().socketFactory,
            certificateCapturingTrustManager,
          )
          if (allowInsecureConnection) {
            httpClientBuilder.hostnameVerifier(HostnameVerifier { _, _ -> true })
          }

          val httpClient = httpClientBuilder.build()
          val request = Request.Builder().url(url).build()
          val response = httpClient.newCall(request).execute()
          val httpResponse =
            HttpResponse(
              certificates = certificateCapturingTrustManager.serverCertificates,
              protocol = OkHttpClientUtils.toDisplayableString(response.protocol),
              statusCode = response.code,
              statusMessage = OkHttpClientUtils.toStatusMessage(response.code) ?: "",
              headers = response.headers.toMultimap(),
              body = response.body.string(),
            )
          onSuccess(httpResponse)
        }

        override fun onCancel() {
          onCancel()
        }

        override fun onThrowable(error: Throwable) {
          onThrowable(error)
        }
      }
      .queue()
  }

  // -- Inner Type ---------------------------------------------------------- //

  private abstract class ExportCertificateAction(
    private val formatName: String,
    private val certificates: List<Certificate>,
    private val url: String,
  ) :
    AnAction(
      UiToolsBundle.message("server-certificates.export-action-title", formatName),
      null,
      AllIcons.Actions.MenuSaveall,
    ) {

    override fun actionPerformed(e: AnActionEvent) {
      try {
        val fileSaverDescriptor = FileSaverDescriptor(e.presentation.text, "")
        val saveFileDialog: FileSaverDialog =
          FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, e.project)
        val defaultFileName = createDefaultCertificateFileName()
        saveFileDialog.save(defaultFileName)?.file?.toPath()?.let { targetPath ->
          Files.write(
            targetPath,
            createFileContent(),
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE,
          )
        }
        onSuccess(e)
      } catch (exception: Exception) {
        val errorMessage = exception.message ?: ""
        Messages.showErrorDialog(
          e.project,
          UiToolsBundle.message("server-certificates.export-failed", errorMessage),
          e.presentation.text,
        )
      }
    }

    open fun onSuccess(e: AnActionEvent) {
      // Override if needed
    }

    abstract fun createFileContent(): ByteArray

    fun X509Certificate.getCn(): String? =
      subjectX500Principal?.name?.let {
        Regex("CN=(?<cn>[^,]+)").find(it)?.groups?.get("cn")?.value
      }

    private fun createDefaultCertificateFileName(): String {
      val fileName =
        if (certificates.size > 1) {
          try {
            "server_certificates_chain_${URI.create(url).host.makeSafeForFilename()}"
          } catch (_: Exception) {
            "server_certificates_chain"
          }
        } else {
          certificates[0].safeCastTo<X509Certificate>()?.getCn()?.makeSafeForFilename()
            ?: "server_certificate"
        }
      return "$fileName.${formatName.lowercase()}"
    }

    private fun String.makeSafeForFilename(): String = this.replace(Regex("[^a-zA-Z0-9]+"), "_")
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ExportAsPemAction(private val certificates: List<Certificate>, url: String) :
    ExportCertificateAction("PEM", certificates, url) {

    override fun createFileContent(): ByteArray =
      certificates
        .flatMap { cert ->
          listOf(
            "-----BEGIN CERTIFICATE-----",
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(cert.encoded),
            "-----END CERTIFICATE-----",
          )
        }
        .joinToString(System.lineSeparator())
        .toByteArray(StandardCharsets.UTF_8)
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ExportAsDerAction(private val certificates: List<Certificate>, url: String) :
    ExportCertificateAction("DER", certificates, url) {

    override fun createFileContent(): ByteArray =
      certificates.flatMap { it.encoded.asList() }.toByteArray()
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ExportAsJksAction(private val certificates: List<Certificate>, url: String) :
    ExportCertificateAction("JKS", certificates, url) {

    private val password = "changeit"

    override fun createFileContent(): ByteArray {
      val keyStore = KeyStore.getInstance("JKS")
      keyStore.load(null, password.toCharArray())

      certificates.forEachIndexed { index, certificate ->
        val alias =
          certificate.safeCastTo<X509Certificate>()?.getCn() ?: "server-certificate-$index"
        keyStore.setCertificateEntry(alias, certificate)
      }

      val outputStream = ByteArrayOutputStream()
      outputStream.use { os -> keyStore.store(os, password.toCharArray()) }
      return outputStream.toByteArray()
    }

    override fun onSuccess(e: AnActionEvent) {
      Messages.showInfoMessage(
        e.project,
        UiToolsBundle.message("server-certificates.export-jks-result", password),
        e.presentation.text,
      )
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class CopyAsPemToClipboardAction(private val certificates: List<Certificate>) :
    AnAction(
      UiToolsBundle.message("server-certificates.copy-pem-to-clipboard-action-title"),
      null,
      AllIcons.Actions.Copy,
    ) {

    override fun actionPerformed(e: AnActionEvent) {
      val pemFile = toPemFile(certificates)
      CopyPasteManager.getInstance().setContents(StringSelection(pemFile))
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ShowAsPemAction(
    private val certificates: List<Certificate>,
    private val context: DeveloperUiToolContext,
    private val configuration: DeveloperToolConfiguration,
    private val project: Project?,
    private val parentDisposable: Disposable,
    private val parentComponent: () -> JComponent,
  ) : AnAction(UiToolsBundle.message("server-certificates.show-as-pem-action-title"), null, null) {

    override fun actionPerformed(e: AnActionEvent) {
      val content = panel {
        row {
            cell(
                AdvancedEditor(
                    id = "server-certificates-show-certificates-as-pem",
                    context = context,
                    configuration = configuration,
                    project = project,
                    title = null,
                    editorMode = OUTPUT,
                    parentDisposable = parentDisposable,
                  )
                  .apply { text = toPemFile(certificates) }
                  .component
              )
              .resizableColumn()
              .align(Align.FILL)
          }
          .resizableRow()
      }
      createPopup(content).show(RelativePoint.getSouthOf(parentComponent()), Balloon.Position.below)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ShowCertificateDetailsAction(
    private val certificates: List<Certificate>,
    private val context: DeveloperUiToolContext,
    private val configuration: DeveloperToolConfiguration,
    private val project: Project?,
    private val parentDisposable: Disposable,
    private val parentComponent: () -> JComponent,
  ) :
    AnAction(
      UiToolsBundle.message("server-certificates.show-certificate-details-action-title"),
      null,
      null,
    ) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = certificates.size == 1
    }

    override fun actionPerformed(e: AnActionEvent) {
      val content = panel {
        row {
            cell(
                AdvancedEditor(
                    id = "server-certificates-show-details",
                    context = context,
                    configuration = configuration,
                    project = project,
                    title = null,
                    editorMode = OUTPUT,
                    parentDisposable = parentDisposable,
                  )
                  .apply { text = certificates[0].toString() }
                  .component
              )
              .resizableColumn()
              .align(Align.FILL)
          }
          .resizableRow()
      }
      createPopup(content).showInCenterOf(parentComponent())
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private data class HttpResponse(
    val certificates: List<Certificate>?,
    val protocol: String,
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, List<String?>>,
    val body: String?,
  )

  // -- Inner Type ---------------------------------------------------------- //

  private class CertificateCapturingTrustManager(private val allowInsecureConnection: Boolean) :
    X509TrustManager {
    val serverCertificates = mutableListOf<Certificate>()

    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {
      // Nothing to do
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {
      serverCertificates.clear()
      chain?.forEach { serverCertificates.add(it) }

      if (!allowInsecureConnection) {
        try {
          val defaultTrustManager =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
          defaultTrustManager.init(null as KeyStore?)
          val defaultX509TrustManager =
            defaultTrustManager.trustManagers.firstOrNull() as? X509TrustManager
              ?: throw IllegalStateException("Default trust manager not available")
          defaultX509TrustManager.checkServerTrusted(chain, authType)
        } catch (e: Exception) {
          throw IllegalStateException("Failed to validate server certificate: ${e.message}", e)
        }
      }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

    fun createSslContext(): SSLContext =
      with(SSLContext.getInstance("TLS")) {
        init(null, arrayOf<TrustManager>(this@CertificateCapturingTrustManager), SecureRandom())
        this
      }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<ServerCertificates> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("server-certificates.menu-title"),
        contentTitle = UiToolsBundle.message("server-certificates.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> ServerCertificates) = { configuration ->
      ServerCertificates(project, context, configuration, parentDisposable)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    fun toPemFile(certificates: List<Certificate>) =
      certificates
        .flatMap { cert ->
          listOf(
            "-----BEGIN CERTIFICATE-----",
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(cert.encoded),
            "-----END CERTIFICATE-----",
          )
        }
        .joinToString(System.lineSeparator())
  }
}
