package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.converter.dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.intellij.openapi.Disposable
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.TestDisposable
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.CliCommandConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@TestApplication
class CliCommandConverterTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  @TestDisposable
  lateinit var disposable: Disposable

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @Test
  fun `test toTarget()`() {
    val cliCommandConverter = CliCommandConverter(DeveloperToolConfiguration("Test"), disposable)
    cliCommandConverter.toTarget("  app -foo   --baz ---baz     -foo-bar \"foo -baz\"   '-foo-bar'")
    val actual = cliCommandConverter.targetText()
    assertThat(actual).isEqualTo("""
app \
  -foo \
  --baz \
  ---baz \
  -foo-bar "foo -baz" '-foo-bar'
    """.trimIndent())
  }

  @Test
  fun `test toSource()`() {
    val cliCommandConverter = CliCommandConverter(DeveloperToolConfiguration("Test"), disposable)
    cliCommandConverter.toSource("""
app \
  -foo \
  --baz \
  ---baz \
  -foo-bar "foo -baz" '-foo-bar'
    """.trimIndent())
    val actual = cliCommandConverter.sourceText()
    assertThat(actual).isEqualTo("app -foo --baz ---baz -foo-bar \"foo -baz\" '-foo-bar'")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}