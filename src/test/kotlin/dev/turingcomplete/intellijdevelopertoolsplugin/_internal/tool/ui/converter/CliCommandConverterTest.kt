package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.TestDisposable
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

@TestApplication
class CliCommandConverterTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  @TestDisposable
  lateinit var disposable: Disposable

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @Test
  fun `test toTarget()`() {
    val cliCommandConverter = CliCommandConverter(DeveloperToolConfiguration("Test", UUID.randomUUID(), emptyMap()), disposable, context, null)
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
    val cliCommandConverter = CliCommandConverter(DeveloperToolConfiguration("Test", UUID.randomUUID(), emptyMap()), disposable, context, null)
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

  companion object {

    private val context = DeveloperUiToolContext("cli-command-converter", true)
  }
}