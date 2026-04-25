package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.jwtencoderdecoder

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.ui.IconManager
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.capitalize
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Objects
import java.util.StringJoiner
import javax.swing.Icon

internal class JwtEditorHighlighter(
  parentDisposable: Disposable,
  private val encodedText: ValueProperty<String>,
  private val highlightingAttributes: TextAttributes?,
) {

  private val highlightEncodedAlarm = Alarm(parentDisposable)
  private val highlightHeaderAlarm = Alarm(parentDisposable)
  private val highlightPayloadAlarm = Alarm(parentDisposable)

  fun refresh(
    encodedEditor: AdvancedEditor,
    headerEditor: AdvancedEditor,
    payloadEditor: AdvancedEditor,
    isToolDisposed: Boolean,
  ) {
    scheduleDotSeparatorHighlight(encodedEditor, isToolDisposed)
    scheduleHeaderClaimsHighlight(headerEditor, isToolDisposed)
    schedulePayloadClaimsHighlight(payloadEditor, isToolDisposed)
  }

  fun scheduleDotSeparatorHighlight(editor: AdvancedEditor, isToolDisposed: Boolean) {
    if (!isToolDisposed && !highlightEncodedAlarm.isDisposed) {
      highlightEncodedAlarm.cancelAllRequests()
      highlightEncodedAlarm.addRequest({ highlightDotSeparatorsNow(editor) }, 100)
    }
  }

  fun highlightDotSeparatorsNow(editor: AdvancedEditor) {
    editor.removeTextRangeHighlighters(ENCODED_DOT_SEPARATOR_GROUP_ID)
    val encoded = encodedText.get()
    var dotIndex = encoded.indexOf('.')
    while (dotIndex != -1) {
      editor.highlightTextRange(
        TextRange(dotIndex, dotIndex + 1),
        ENCODED_DOT_SEPARATOR_HIGHLIGHTER_LAYER,
        highlightingAttributes,
        ENCODED_DOT_SEPARATOR_GROUP_ID,
      )
      dotIndex = encoded.indexOf('.', dotIndex + 1)
    }
  }

  fun scheduleHeaderClaimsHighlight(editor: AdvancedEditor, isToolDisposed: Boolean) {
    if (!isToolDisposed && !highlightHeaderAlarm.isDisposed) {
      highlightHeaderAlarm.cancelAllRequests()
      highlightHeaderAlarm.addRequest({ highlightClaims(editor) }, 100)
    }
  }

  fun schedulePayloadClaimsHighlight(editor: AdvancedEditor, isToolDisposed: Boolean) {
    if (!isToolDisposed && !highlightPayloadAlarm.isDisposed) {
      highlightPayloadAlarm.cancelAllRequests()
      highlightPayloadAlarm.addRequest({ highlightClaims(editor) }, 100)
    }
  }

  private fun highlightClaims(editor: AdvancedEditor) {
    editor.removeTextRangeHighlighters(HEADER_PAYLOAD_HIGHLIGHT_GROUP_ID)

    JwtEncoderDecoder.unixTimestampSecondsJsonValueRegex.findAll(editor.text).forEach {
      val unixTimestampSecondsMatch = it.groups[1]
      if (unixTimestampSecondsMatch != null) {
        val textRange =
          TextRange(unixTimestampSecondsMatch.range.first, unixTimestampSecondsMatch.range.last + 1)
        editor.highlightTextRange(
          textRange,
          UNIX_TIMESTAMP_HIGHLIGHT_LAYER,
          null,
          HEADER_PAYLOAD_HIGHLIGHT_GROUP_ID,
          ClaimFeatureUnixTimestampGutterIconRenderer(
            textRange,
            unixTimestampSecondsMatch.value.toLong(),
          ),
        )
      }
    }

    JwtEncoderDecoder.claimRegex
      .findAll(editor.text)
      .mapNotNull {
        val claimMatch = it.groups[1]
        if (claimMatch != null) {
          val standardClaim = StandardClaim.findByFieldName(claimMatch.value)
          if (standardClaim != null) {
            return@mapNotNull claimMatch.range to standardClaim
          }
        }
        null
      }
      .forEach { (claimRange, standardClaim) ->
        val textRange = TextRange(claimRange.first, claimRange.last + 1)
        editor.highlightTextRange(
          textRange,
          CLAIM_REGEX_MATCH_HIGHLIGHT_LAYER,
          null,
          HEADER_PAYLOAD_HIGHLIGHT_GROUP_ID,
          StandardClaimGutterIconRenderer(textRange, standardClaim),
        )
      }
  }

  companion object {

    private const val HEADER_PAYLOAD_HIGHLIGHT_GROUP_ID = "claims"
    private const val ENCODED_DOT_SEPARATOR_GROUP_ID = "encodedDotSeparator"
    private const val ENCODED_DOT_SEPARATOR_HIGHLIGHTER_LAYER = HighlighterLayer.SELECTION - 1
    private const val UNIX_TIMESTAMP_HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 1
    private const val CLAIM_REGEX_MATCH_HIGHLIGHT_LAYER = UNIX_TIMESTAMP_HIGHLIGHT_LAYER - 1
  }
}

internal class StandardClaimGutterIconRenderer(
  private val textRange: TextRange,
  private val standardClaim: StandardClaim,
) : GutterIconRenderer() {

  override fun getTooltipText(): String = standardClaim.toString()

  override fun getIcon(): Icon = AllIcons.Gutter.JavadocRead

  override fun equals(other: Any?): Boolean {
    return if (other != null && other is StandardClaimGutterIconRenderer) {
      other.textRange == textRange && other.standardClaim == standardClaim
    } else {
      false
    }
  }

  override fun hashCode(): Int = Objects.hash(textRange, standardClaim)

  override fun getAlignment(): Alignment = Alignment.RIGHT
}

internal class ClaimFeatureUnixTimestampGutterIconRenderer(
  private val textRange: TextRange,
  private val unixTimestampSeconds: Long,
) : GutterIconRenderer() {

  override fun getTooltipText(): String {
    val tooltipText = StringJoiner("<br /><br />")

    tooltipText.add(
      Instant.ofEpochSecond(unixTimestampSeconds)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    )

    val diff =
      DateFormatUtil.formatBetweenDates(
        unixTimestampSeconds.times(1000),
        System.currentTimeMillis(),
      )
    tooltipText.add("${diff.capitalize()}.")

    return tooltipText.toString()
  }

  override fun getIcon(): Icon = clockGutterIcon

  override fun equals(other: Any?): Boolean {
    return if (other != null && other is ClaimFeatureUnixTimestampGutterIconRenderer) {
      other.textRange == textRange && other.unixTimestampSeconds == unixTimestampSeconds
    } else {
      false
    }
  }

  override fun hashCode(): Int = Objects.hash(textRange, unixTimestampSeconds)

  override fun getAlignment(): Alignment = Alignment.LEFT

  companion object {

    private val clockGutterIcon =
      IconManager.getInstance()
        .getIcon(
          "dev/turingcomplete/intellijdevelopertoolsplugin/icons/clock_gutter.svg",
          ClaimFeatureUnixTimestampGutterIconRenderer::class.java.classLoader,
        )
  }
}
