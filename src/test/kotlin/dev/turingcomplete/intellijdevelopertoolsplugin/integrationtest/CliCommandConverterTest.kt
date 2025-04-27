package dev.turingcomplete.intellijdevelopertoolsplugin.integrationtest

import com.intellij.openapi.Disposable
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.TestDisposable
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.CliCommandConverter
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@TestApplication
class CliCommandConverterTest {
  // -- Properties ---------------------------------------------------------- //

  @TestDisposable lateinit var disposable: Disposable

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @Test
  fun `test toTarget()`() {
    val cliCommandConverter =
      CliCommandConverter(
        DeveloperToolConfiguration("Test", UUID.randomUUID(), emptyMap()),
        disposable,
        context,
        null,
      )
    val actual =
      String(
        cliCommandConverter.doConvertToTarget(
          "  app -foo   --baz ---baz     -foo-bar \"foo -baz\"   '-foo-bar'".toByteArray()
        )
      )
    assertThat(actual)
      .isEqualTo(
        """
app \
  -foo \
  --baz \
  ---baz \
  -foo-bar "foo -baz" '-foo-bar'
    """
          .trimIndent()
      )
  }

  @Test
  fun `test toSource()`() {
    val cliCommandConverter =
      CliCommandConverter(
        DeveloperToolConfiguration("Test", UUID.randomUUID(), emptyMap()),
        disposable,
        context,
        null,
      )
    val actual =
      cliCommandConverter.doConvertToSource(
        """
app \
  -foo \
  --baz \
  ---baz \
  -foo-bar "foo -baz" '-foo-bar'
    """
          .trimIndent()
          .toByteArray()
      )
    assertThat(String(actual)).isEqualTo("app -foo --baz ---baz -foo-bar \"foo -baz\" '-foo-bar'")
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val context = DeveloperUiToolContext("cli-command-converter", true)
  }
}
