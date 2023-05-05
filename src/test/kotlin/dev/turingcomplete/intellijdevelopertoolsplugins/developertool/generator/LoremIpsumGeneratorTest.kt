package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.generator

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.LoremIpsumGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LoremIpsumGeneratorTest(
  private val atMostWords: Int,
  private val expectedSentence: String
) : BasePlatformTestCase() {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Test
  fun `test generation of iconic sentence`() {
    val actualSentence = LoremIpsumGenerator(DeveloperToolConfiguration("Test")) { }.generateIconicText(atMostWords, true)
    assertThat(actualSentence.joinToString(" ")).isEqualTo(expectedSentence)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    @Parameterized.Parameters(name = "{0}, {1}")
    @JvmStatic
    fun parameters(): List<Array<Any>> {
      return listOf(
              arrayOf(0, ""),
              arrayOf(1, "Lorem."),
              arrayOf(2, "Lorem ipsum."),
              arrayOf(3, "Lorem ipsum dolor."),
              arrayOf(4, "Lorem ipsum dolor sit."),
              arrayOf(5, "Lorem ipsum dolor sit amet."),
              arrayOf(6, "Lorem ipsum dolor sit amet, consectetur."),
              arrayOf(7, "Lorem ipsum dolor sit amet, consectetur adipiscing."),
              arrayOf(8, "Lorem ipsum dolor sit amet, consectetur adipiscing elit."),
      )
    }
  }
}