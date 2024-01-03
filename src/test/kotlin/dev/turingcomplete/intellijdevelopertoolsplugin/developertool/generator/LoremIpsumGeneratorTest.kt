package dev.turingcomplete.intellijdevelopertoolsplugin.developertool.generator

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.junit5.TestApplication
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.generator.LoremIpsumGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@TestApplication
class LoremIpsumGeneratorTest : BasePlatformTestCase() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @ParameterizedTest
  @CsvSource(
    delimiter = '|',
    value = [
      "0|",
      "1|Lorem.",
      "2|Lorem ipsum.",
      "3|Lorem ipsum dolor.",
      "4|Lorem ipsum dolor sit.",
      "5|Lorem ipsum dolor sit amet.",
      "6|Lorem ipsum dolor sit amet, consectetur.",
      "7|Lorem ipsum dolor sit amet, consectetur adipiscing.",
      "8|Lorem ipsum dolor sit amet, consectetur adipiscing elit."
    ]
  )
  fun `test generation of iconic sentence`(atMostWords: Int, expectedSentence: String?) {
    val actualSentence = LoremIpsumGenerator(null, DeveloperUiToolContext("lorem-ipsum-generator", true), DeveloperToolConfiguration("Test")) { }
      .generateIconicText(atMostWords, true)
    assertThat(actualSentence.joinToString(" ")).isEqualTo(expectedSentence ?: "")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}