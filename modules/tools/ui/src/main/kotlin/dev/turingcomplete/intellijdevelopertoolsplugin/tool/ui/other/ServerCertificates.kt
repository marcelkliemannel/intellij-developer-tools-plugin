package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.awt.RelativePoint
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
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.capitalize
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
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils.Popup.createPopup
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils.createWrappingTextArea
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Date
import java.util.StringJoiner
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent
import okhttp3.Call
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
  private val fetchingServerCertificates = ValueProperty(false)
  private val certificatesPanel = BorderLayoutPanel()

  @Volatile private var lastSuccessfulHttpResponse: HttpResponse? = null

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
          val normalizedUrl = normalizeUrl(url.get())
          url.set(normalizedUrl)
          fetchCertificates(
            project = project,
            url = normalizedUrl,
            allowInsecureConnection = allowInsecureConnection.get(),
            onStarted = {
              setFetchingServerCertificates(true)
              setCertificatesResultUi {
                createResultUi(createFetchingUi(), lastSuccessfulHttpResponse)
              }
            },
            onSuccess = {
              lastSuccessfulHttpResponse = it
              setFetchingServerCertificates(false)
              setCertificatesResultUi { createCertificatesUi(it) }
            },
            onCancel = {
              setFetchingServerCertificates(false)
              setCertificatesResultUi {
                lastSuccessfulHttpResponse?.let { createCertificatesUi(it) }
              }
            },
            onThrowable = { e ->
              log.warn("Failed to retrieve server certificates from: $normalizedUrl", e)
              setFetchingServerCertificates(false)
              setCertificatesResultUi {
                createResultUi(createFetchingFailedUi(e), lastSuccessfulHttpResponse)
              }
            },
          )
        }
        .enabledIf(PropertyComponentPredicate(fetchingServerCertificates, false))
    }

    row { cell(certificatesPanel).resizableColumn().align(Align.FILL) }
      .resizableRow()
      .topGap(TopGap.MEDIUM)
  }

  private fun setCertificatesResultUi(componentFactory: () -> JComponent?) {
    ApplicationManager.getApplication().invokeLater {
      val component = componentFactory()
      certificatesPanel.removeAll()
      if (component != null) {
        certificatesPanel.addToCenter(component)
      }
      certificatesPanel.revalidate()
      certificatesPanel.repaint()

      scrollToTop()
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun setFetchingServerCertificates(fetching: Boolean) {
    ApplicationManager.getApplication().invokeLater { fetchingServerCertificates.set(fetching) }
  }

  private fun normalizeUrl(input: String): String {
    val trimmedInput = input.trim()
    return if ("://" in trimmedInput) trimmedInput else "https://$trimmedInput"
  }

  private fun createResultUi(
    statusComponent: JComponent?,
    previousResponse: HttpResponse?,
  ): JComponent? =
    when {
      statusComponent == null && previousResponse == null -> null
      previousResponse == null -> statusComponent
      else ->
        panel {
          if (statusComponent != null) {
            row { cell(statusComponent).resizableColumn().align(Align.FILL) }
          }
          row { cell(createCertificatesUi(previousResponse)).resizableColumn().align(Align.FILL) }
            .resizableRow()
        }
    }

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
    buildResponseSummaryUi(httpResponse)

    if (httpResponse.certificates?.isNotEmpty() == true) {
      buildCertificatesExportUi(httpResponse.certificates)

      httpResponse.certificates.forEachIndexed { index, certificate ->
        val certificateRole = getCertificateRole(index, httpResponse.certificates)
        group(
          UiToolsBundle.message(
            "server-certificates.certificate-title-with-role",
            index + 1,
            certificateRole,
          ),
          false,
        ) {
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

  private fun Panel.buildResponseSummaryUi(httpResponse: HttpResponse) {
    group(UiToolsBundle.message("server-certificates.summary"), false) {
      row(UiToolsBundle.message("server-certificates.response.title")) {
        cell(
          HyperlinkLabel(
              UiToolsBundle.message(
                "server-certificates.response.status",
                httpResponse.statusCode,
                httpResponse.statusMessage,
              )
            )
            .apply {
              addHyperlinkListener(createShowHttpResponseHyperlinkHandler(httpResponse, this))
            }
        )
      }
      buildCopyablePropertyRow(
        UiToolsBundle.message("server-certificates.summary-requested-url"),
        httpResponse.requestedUrl,
      )
      if (httpResponse.finalUrl != httpResponse.requestedUrl) {
        buildCopyablePropertyRow(
          UiToolsBundle.message("server-certificates.summary-final-url"),
          httpResponse.finalUrl,
        )
      }
      buildCopyablePropertyRow(
        UiToolsBundle.message("server-certificates.summary-certificates-count"),
        (httpResponse.certificates?.size ?: 0).toString(),
      )
      buildCopyablePropertyRow(
        UiToolsBundle.message("server-certificates.summary-tls-version"),
        httpResponse.tlsVersion ?: UiToolsBundle.message("server-certificates.not-available"),
      )
      buildCopyablePropertyRow(
        UiToolsBundle.message("server-certificates.summary-cipher-suite"),
        httpResponse.cipherSuite ?: UiToolsBundle.message("server-certificates.not-available"),
      )
      buildCopyablePropertyRow(
        UiToolsBundle.message("server-certificates.summary-trust-status"),
        httpResponse.trustStatus.toDisplayText(),
      )
      val hostname = URI.create(httpResponse.finalUrl).host
      val leafCertificate = httpResponse.certificates?.firstOrNull() as? X509Certificate
      if (hostname != null && leafCertificate != null) {
        buildCopyablePropertyRow(
          UiToolsBundle.message("server-certificates.summary-hostname-status"),
          if (leafCertificate.matchesHostname(hostname)) {
            UiToolsBundle.message("server-certificates.hostname-status-matches", hostname)
          } else {
            UiToolsBundle.message("server-certificates.hostname-status-does-not-match", hostname)
          },
        )
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
                            "${httpResponse.protocol} ${httpResponse.statusCode} " +
                              httpResponse.statusMessage
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
        UiToolsBundle.message("server-certificates.certificate-fingerprint-sha256") to
          certificate.getSha256Fingerprint(),
      )
      .forEach { (title, value) ->
        buildCopyablePropertyRow(title, value.toCertificatePropertyString())
      }

    val subjectAlternativeNames = certificate.getDisplayableSubjectAlternativeNames()
    if (subjectAlternativeNames.isNotEmpty()) {
      buildCopyablePropertyRow(
        UiToolsBundle.message("server-certificates.certificate-subject-alternative-names"),
        subjectAlternativeNames.joinToString(", "),
      )
    }
  }

  private fun Panel.buildCertificateValidityUi(certificate: X509Certificate) {
    val validityStatus =
      try {
        certificate.checkValidity()
        val millisUntilExpiry = certificate.notAfter.time - System.currentTimeMillis()
        val timeUntilExpiry =
          DateFormatUtil.formatBetweenDates(certificate.notAfter.time, System.currentTimeMillis())
            .capitalize()
        if (millisUntilExpiry <= CERTIFICATE_EXPIRING_SOON_THRESHOLD_MILLIS) {
          UiToolsBundle.message("server-certificates.certificate-expires-soon", timeUntilExpiry)
        } else {
          UiToolsBundle.message("server-certificates.certificate-valid", timeUntilExpiry)
        }
      } catch (_: CertificateExpiredException) {
        UiToolsBundle.message("server-certificates.certificate-expired")
      } catch (_: CertificateNotYetValidException) {
        UiToolsBundle.message("server-certificates.certificate-not-valid-yet")
      }

    buildCopyablePropertyRow(
      UiToolsBundle.message("server-certificates.certificate-validity"),
      validityStatus,
    )
  }

  private fun Panel.buildCopyablePropertyRow(title: String, value: String) {
    row("$title:") {
      cell(createWrappingTextArea(value))
        .resizableColumn() // keep this
        .align(Align.FILL)
        .gap(RightGap.SMALL)
        .applyToComponent {
          // ensures it reflows when shrinking
          maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
    }
  }

  private fun Any.toCertificatePropertyString(): String =
    when (this) {
      is String -> this
      is BigInteger -> toString(16).uppercase()
      is X500Principal -> toString()
      is Date -> {
        val diff = DateFormatUtil.formatBetweenDates(time, System.currentTimeMillis())
        "${DateFormatUtil.formatDateTime(this)} (${diff.capitalize()})"
      }

      else -> throw IllegalStateException("Unknown property type: ${this::class}")
    }

  private fun X509Certificate.getDisplayableSubjectAlternativeNames(): List<String> =
    getSubjectAlternativeNamesWithTypes().mapNotNull { (type, value) ->
      when (type) {
        SUBJECT_ALTERNATIVE_NAME_DNS ->
          "${UiToolsBundle.message("server-certificates.certificate-san-dns")}: $value"

        SUBJECT_ALTERNATIVE_NAME_IP ->
          "${UiToolsBundle.message("server-certificates.certificate-san-ip")}: $value"

        else -> null
      }
    }

  private fun X509Certificate.getSubjectAlternativeNamesWithTypes(): List<Pair<Int, String>> =
    try {
      subjectAlternativeNames
        ?.mapNotNull { subjectAlternativeName ->
          val type = subjectAlternativeName.getOrNull(0) as? Int ?: return@mapNotNull null
          val value = subjectAlternativeName.getOrNull(1)?.toString() ?: return@mapNotNull null
          type to value
        }
        .orEmpty()
    } catch (_: Exception) {
      emptyList()
    }

  private fun X509Certificate.matchesHostname(hostname: String): Boolean {
    val normalizedHostname = hostname.lowercase()
    val subjectAlternativeNames = getSubjectAlternativeNamesWithTypes()
    val ipSubjectAlternativeNames =
      subjectAlternativeNames
        .filter { it.first == SUBJECT_ALTERNATIVE_NAME_IP }
        .map { it.second.lowercase() }
    val dnsSubjectAlternativeNames =
      subjectAlternativeNames
        .filter { it.first == SUBJECT_ALTERNATIVE_NAME_DNS }
        .map { it.second.lowercase() }

    return when {
      ipSubjectAlternativeNames.isNotEmpty() ->
        ipSubjectAlternativeNames.any { it == normalizedHostname }

      dnsSubjectAlternativeNames.isNotEmpty() ->
        dnsSubjectAlternativeNames.any { it.matchesDnsName(normalizedHostname) }

      else -> getCommonName()?.lowercase()?.matchesDnsName(normalizedHostname) == true
    }
  }

  private fun X509Certificate.getCommonName(): String? =
    subjectX500Principal?.name?.let { Regex("CN=(?<cn>[^,]+)").find(it)?.groups?.get("cn")?.value }

  private fun String.matchesDnsName(hostname: String): Boolean =
    if (startsWith("*.")) {
      val suffix = substring(1)
      val prefix = hostname.removeSuffix(suffix)
      hostname.endsWith(suffix) && prefix.isNotEmpty() && "." !in prefix
    } else {
      this == hostname
    }

  private fun X509Certificate.getSha256Fingerprint(): String =
    MessageDigest.getInstance("SHA-256").digest(encoded).joinToString(":") {
      "%02X".format(it.toInt() and 0xff)
    }

  private fun getCertificateRole(index: Int, certificates: List<Certificate>): String {
    if (index == 0) {
      return UiToolsBundle.message("server-certificates.certificate-role-leaf")
    }

    val x509Certificate = certificates[index].safeCastTo<X509Certificate>()
    return if (
      index == certificates.lastIndex &&
        x509Certificate?.subjectX500Principal == x509Certificate?.issuerX500Principal
    ) {
      UiToolsBundle.message("server-certificates.certificate-role-root")
    } else {
      UiToolsBundle.message("server-certificates.certificate-role-intermediate")
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
        @Volatile private var call: Call? = null

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
            httpClientBuilder.hostnameVerifier { _, _ -> true }
          }

          val httpClient = httpClientBuilder.build()
          val request = Request.Builder().url(url).build()
          val call = httpClient.newCall(request)
          this.call = call
          try {
            call.execute().use { response ->
              val httpResponse =
                HttpResponse(
                  certificates = certificateCapturingTrustManager.serverCertificates,
                  requestedUrl = url,
                  finalUrl = response.request.url.toString(),
                  protocol = OkHttpClientUtils.toDisplayableString(response.protocol),
                  statusCode = response.code,
                  statusMessage =
                    response.message.ifBlank {
                      OkHttpClientUtils.toStatusMessage(response.code) ?: ""
                    },
                  tlsVersion = response.handshake?.tlsVersion?.javaName,
                  cipherSuite = response.handshake?.cipherSuite?.javaName,
                  trustStatus = certificateCapturingTrustManager.getCertificateTrustStatus(),
                  headers = response.headers.toMultimap(),
                  body = response.peekBody(MAX_RESPONSE_BODY_BYTES).string(),
                )
              onSuccess(httpResponse)
            }
          } catch (e: IOException) {
            if (call.isCanceled()) {
              throw ProcessCanceledException(e)
            }
            throw e
          } finally {
            this.call = null
          }
        }

        override fun onCancel() {
          call?.cancel()
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
    actionTitle: String =
      UiToolsBundle.message("server-certificates.export-action-title", formatName),
  ) : AnAction(actionTitle, null, AllIcons.Actions.MenuSaveall) {

    override fun actionPerformed(e: AnActionEvent) {
      try {
        val fileSaverDescriptor = FileSaverDescriptor(e.presentation.text, "")
        val saveFileDialog: FileSaverDialog =
          FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, e.project)
        val defaultFileName = createDefaultCertificateFileName()
        val targetPath = saveFileDialog.save(defaultFileName)?.file?.toPath() ?: return
        Files.write(
          targetPath,
          createFileContent(),
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.CREATE,
        )
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

    protected fun String.makeSafeForFilename(): String = this.replace(Regex("[^a-zA-Z0-9]+"), "_")
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ExportAsPemAction(private val certificates: List<Certificate>, url: String) :
    ExportCertificateAction(
      "PEM",
      certificates,
      url,
      if (certificates.size > 1) {
        UiToolsBundle.message("server-certificates.export-chain-as-pem-action-title")
      } else {
        UiToolsBundle.message("server-certificates.export-certificate-as-pem-action-title")
      },
    ) {

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
    ExportCertificateAction(
      "DER",
      certificates,
      url,
      if (certificates.size > 1) {
        UiToolsBundle.message("server-certificates.export-der-files-action-title")
      } else {
        UiToolsBundle.message("server-certificates.export-certificate-as-der-action-title")
      },
    ) {

    override fun actionPerformed(e: AnActionEvent) {
      if (certificates.size == 1) {
        super.actionPerformed(e)
        return
      }

      try {
        val targetDirectory =
          FileChooser.chooseFile(
              FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle(
                  UiToolsBundle.message("server-certificates.export-der-files-directory-title")
                ),
              e.project,
              null,
            )
            ?.toNioPath() ?: return

        certificates.forEachIndexed { index, certificate ->
          Files.write(
            targetDirectory.resolve(createDerFileName(index, certificate)),
            certificate.encoded,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE,
          )
        }
      } catch (exception: Exception) {
        val errorMessage = exception.message ?: ""
        Messages.showErrorDialog(
          e.project,
          UiToolsBundle.message("server-certificates.export-failed", errorMessage),
          e.presentation.text,
        )
      }
    }

    override fun createFileContent(): ByteArray =
      certificates.flatMap { it.encoded.asList() }.toByteArray()

    private fun createDerFileName(index: Int, certificate: Certificate): Path {
      val certificateName =
        certificate.safeCastTo<X509Certificate>()?.getCn()?.makeSafeForFilename()
          ?: "server_certificate"
      return Path.of("${(index + 1).toString().padStart(2, '0')}_$certificateName.der")
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ExportAsJksAction(private val certificates: List<Certificate>, url: String) :
    ExportCertificateAction(
      "JKS",
      certificates,
      url,
      if (certificates.size > 1) {
        UiToolsBundle.message("server-certificates.export-chain-as-jks-action-title")
      } else {
        UiToolsBundle.message("server-certificates.export-certificate-as-jks-action-title")
      },
    ) {

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
      if (certificates.size > 1) {
        UiToolsBundle.message("server-certificates.copy-chain-pem-to-clipboard-action-title")
      } else {
        UiToolsBundle.message("server-certificates.copy-certificate-pem-to-clipboard-action-title")
      },
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
  ) :
    AnAction(
      if (certificates.size > 1) {
        UiToolsBundle.message("server-certificates.show-chain-as-pem-action-title")
      } else {
        UiToolsBundle.message("server-certificates.show-certificate-as-pem-action-title")
      },
      null,
      null,
    ) {

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
    val requestedUrl: String,
    val finalUrl: String,
    val protocol: String,
    val statusCode: Int,
    val statusMessage: String,
    val tlsVersion: String?,
    val cipherSuite: String?,
    val trustStatus: CertificateTrustStatus,
    val headers: Map<String, List<String?>>,
    val body: String?,
  )

  // -- Inner Type ---------------------------------------------------------- //

  private sealed class CertificateTrustStatus {

    data object Trusted : CertificateTrustStatus()

    data class NotTrusted(val errorMessage: String?) : CertificateTrustStatus()

    data object NotValidated : CertificateTrustStatus()

    fun toDisplayText(): String =
      when (this) {
        Trusted -> UiToolsBundle.message("server-certificates.trust-status-trusted")
        is NotTrusted ->
          errorMessage?.let {
            UiToolsBundle.message("server-certificates.trust-status-not-trusted-with-reason", it)
          } ?: UiToolsBundle.message("server-certificates.trust-status-not-trusted")

        NotValidated -> UiToolsBundle.message("server-certificates.trust-status-not-validated")
      }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class CertificateCapturingTrustManager(private val allowInsecureConnection: Boolean) :
    X509TrustManager {
    val serverCertificates = mutableListOf<Certificate>()
    private var certificateTrustStatus: CertificateTrustStatus = CertificateTrustStatus.NotValidated

    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {
      // Nothing to do
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {
      serverCertificates.clear()
      chain?.forEach { serverCertificates.add(it) }

      try {
        val defaultTrustManager =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        defaultTrustManager.init(null as KeyStore?)
        val defaultX509TrustManager =
          defaultTrustManager.trustManagers.firstOrNull() as? X509TrustManager
            ?: throw IllegalStateException("Default trust manager not available")
        defaultX509TrustManager.checkServerTrusted(chain, authType)
        certificateTrustStatus = CertificateTrustStatus.Trusted
      } catch (e: Exception) {
        certificateTrustStatus = CertificateTrustStatus.NotTrusted(e.message)
        if (!allowInsecureConnection) {
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

    fun getCertificateTrustStatus(): CertificateTrustStatus = certificateTrustStatus
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

    private const val MAX_RESPONSE_BODY_BYTES = 64L * 1024L
    private const val CERTIFICATE_EXPIRING_SOON_THRESHOLD_MILLIS = 30L * 24L * 60L * 60L * 1000L
    private const val SUBJECT_ALTERNATIVE_NAME_DNS = 2
    private const val SUBJECT_ALTERNATIVE_NAME_IP = 7

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
