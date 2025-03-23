package dev.turingcomplete.intellijdevelopertoolsplugin.common

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.net.JdkProxyProvider
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.*

object OkHttpClientUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun OkHttpClient.Builder.applyIntelliJProxySettings(url: String): OkHttpClient.Builder {
    val proxies = JdkProxyProvider.getInstance().proxySelector.select(VfsUtil.toUri(url))
    if (proxies.isNotEmpty()) {
      proxySelector(FallbackProxySelector(proxies))
    }

    return this
  }

  fun toDisplayableString(protocol: Protocol): String =
    when (protocol) {
      Protocol.HTTP_1_0 -> "HTTP/1.0"
      Protocol.HTTP_1_1 -> "HTTP/1.1"
      Protocol.HTTP_2 -> "HTTP/2"
      Protocol.H2_PRIOR_KNOWLEDGE -> "HTTP/2 (Prior Knowledge)"
      Protocol.QUIC -> "QUIC"
      Protocol.SPDY_3 -> "SPDY/3.1"
    }

  fun toStatusMessage(statusCode: Int): String? =
    when (statusCode) {
      100 -> "Continue"
      101 -> "Switching Protocols"
      102 -> "Processing"
      200 -> "OK"
      201 -> "Created"
      202 -> "Accepted"
      203 -> "Non-Authoritative Information"
      204 -> "No Content"
      205 -> "Reset Content"
      206 -> "Partial Content"
      207 -> "Multi-Status"
      300 -> "Multiple Choices"
      301 -> "Moved Permanently"
      302 -> "Found"
      303 -> "See Other"
      304 -> "Not Modified"
      305 -> "Use Proxy"
      307 -> "Temporary Redirect"
      308 -> "Permanent Redirect"
      400 -> "Bad Request"
      401 -> "Unauthorized"
      402 -> "Payment Required"
      403 -> "Forbidden"
      404 -> "Not Found"
      405 -> "Method Not Allowed"
      406 -> "Not Acceptable"
      407 -> "Proxy Authentication Required"
      408 -> "Request Timeout"
      409 -> "Conflict"
      410 -> "Gone"
      411 -> "Length Required"
      412 -> "Precondition Failed"
      413 -> "Payload Too Large"
      414 -> "URI Too Long"
      415 -> "Unsupported Media Type"
      416 -> "Range Not Satisfiable"
      417 -> "Expectation Failed"
      418 -> "I'm a teapot"
      421 -> "Misdirected Request"
      422 -> "Unprocessable Entity"
      423 -> "Locked"
      424 -> "Failed Dependency"
      425 -> "Too Early"
      426 -> "Upgrade Required"
      428 -> "Precondition Required"
      429 -> "Too Many Requests"
      431 -> "Request Header Fields Too Large"
      451 -> "Unavailable For Legal Reasons"
      500 -> "Internal Server Error"
      501 -> "Not Implemented"
      502 -> "Bad Gateway"
      503 -> "Service Unavailable"
      504 -> "Gateway Timeout"
      505 -> "HTTP Version Not Supported"
      506 -> "Variant Also Negotiates"
      507 -> "Insufficient Storage"
      508 -> "Loop Detected"
      510 -> "Not Extended"
      511 -> "Network Authentication Required"
      else -> null
    }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class FallbackProxySelector(private val proxies: List<Proxy>) : ProxySelector() {
    private val failedProxies = Collections.synchronizedSet(mutableSetOf<Proxy>())

    override fun select(uri: URI?): List<Proxy> {
      return proxies.filter { !failedProxies.contains(it) }
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
      proxies.find { it.address() == sa }?.let { failedProxies.add(it) }
    }
  }
}
