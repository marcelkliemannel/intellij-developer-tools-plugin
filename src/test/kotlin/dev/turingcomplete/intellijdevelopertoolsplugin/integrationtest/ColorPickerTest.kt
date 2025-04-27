package dev.turingcomplete.intellijdevelopertoolsplugin.integrationtest

import com.intellij.openapi.Disposable
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.TestDisposable
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other.ColorPicker
import java.awt.Color
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@TestApplication
class ColorPickerTest {
  // -- Properties ---------------------------------------------------------- //

  @TestDisposable lateinit var disposable: Disposable

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @ParameterizedTest
  @CsvSource(
    delimiter = '|',
    value =
      [
        "0|0|0|255|rgb(0, 0, 0)|rgba(0, 0, 0, 1)|#000000|#000000FF|hsl(0, 0%, 0%)|hsla(0, 0%, 0%, 1)",
        "255|255|255|255|rgb(255, 255, 255)|rgba(255, 255, 255, 1)|#FFFFFF|#FFFFFFFF|hsl(0, 0%, 100%)|hsla(0, 0%, 100%, 1)",
        "255|0|0|255|rgb(255, 0, 0)|rgba(255, 0, 0, 1)|#FF0000|#FF0000FF|hsl(0, 100%, 50%)|hsla(0, 100%, 50%, 1)",
        "0|255|0|255|rgb(0, 255, 0)|rgba(0, 255, 0, 1)|#00FF00|#00FF00FF|hsl(120, 100%, 50%)|hsla(120, 100%, 50%, 1)",
        "0|0|255|255|rgb(0, 0, 255)|rgba(0, 0, 255, 1)|#0000FF|#0000FFFF|hsl(240, 100%, 50%)|hsla(240, 100%, 50%, 1)",
        "255|255|0|255|rgb(255, 255, 0)|rgba(255, 255, 0, 1)|#FFFF00|#FFFF00FF|hsl(60, 100%, 50%)|hsla(60, 100%, 50%, 1)",
        "0|255|255|255|rgb(0, 255, 255)|rgba(0, 255, 255, 1)|#00FFFF|#00FFFFFF|hsl(180, 100%, 50%)|hsla(180, 100%, 50%, 1)",
        "255|0|255|255|rgb(255, 0, 255)|rgba(255, 0, 255, 1)|#FF00FF|#FF00FFFF|hsl(300, 100%, 50%)|hsla(300, 100%, 50%, 1)",
        "128|128|128|255|rgb(128, 128, 128)|rgba(128, 128, 128, 1)|#808080|#808080FF|hsl(0, 0%, 50.2%)|hsla(0, 0%, 50.2%, 1)",
        "195|211|15|168|rgb(195, 211, 15)|rgba(195, 211, 15, 0.66)|#C3D30F|#C3D30FA8|hsl(64.9, 86.73%, 44.31%)|hsla(64.9, 86.73%, 44.31%, 0.66)",
        "0|0|0|0|rgb(0, 0, 0)|rgba(0, 0, 0, 0)|#000000|#00000000|hsl(0, 0%, 0%)|hsla(0, 0%, 0%, 0)",
        "255|255|255|0|rgb(255, 255, 255)|rgba(255, 255, 255, 0)|#FFFFFF|#FFFFFF00|hsl(0, 0%, 100%)|hsla(0, 0%, 100%, 0)",
        "123|45|67|128|rgb(123, 45, 67)|rgba(123, 45, 67, 0.5)|#7B2D43|#7B2D4380|hsl(343.08, 46.43%, 32.94%)|hsla(343.08, 46.43%, 32.94%, 0.5)",
      ],
  )
  fun `test RGB conversion`(
    r: Int,
    g: Int,
    b: Int,
    a: Int,
    expectedRgb: String,
    expectedRgba: String,
    expectedHex: String,
    expectedHexAlpha: String,
    expectedHsl: String,
    expectedHsla: String,
  ) {
    val colorPicker =
      ColorPicker(
        null,
        DeveloperToolConfiguration("Test", UUID.randomUUID(), emptyMap()),
        disposable,
      )

    val cssValues = colorPicker.createCssValues(Color(r, g, b, a))
    assertThat(cssValues.rgb).isEqualTo(expectedRgb)
    assertThat(cssValues.rgbWithAlpha).isEqualTo(expectedRgba)
    assertThat(cssValues.hex).isEqualTo(expectedHex)
    assertThat(cssValues.hexWithAlpha).isEqualTo(expectedHexAlpha)
    assertThat(cssValues.hls).isEqualTo(expectedHsl)
    assertThat(cssValues.hlsWithAlpha).isEqualTo(expectedHsla)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
