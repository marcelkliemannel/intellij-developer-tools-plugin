package dev.turingcomplete.intellijdevelopertoolsplugin.common.message

import java.util.Properties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CommonBundleTest {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @ParameterizedTest
  @ValueSource(strings = ["", "_de"])
  fun `list-separator-comma has a trailing space`(bundleFilePostfix: String) {
    val value = readProperty(bundleFilePostfix = bundleFilePostfix, key = "list.separator.comma")
    assertThat(value).endsWith(" ")
  }

  @ParameterizedTest
  @ValueSource(strings = ["", "_de"])
  fun `list-conjunction-and has a leading an trailing spaces`(bundleFilePostfix: String) {
    val value = readProperty(bundleFilePostfix = bundleFilePostfix, key = "list.conjunction.and")
    assertThat(value).startsWith(" ").endsWith(" ")
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun readProperty(bundleFilePostfix: String, key: String): String {
    val properties = Properties()
    CommonBundleTest::class
      .java
      .getResourceAsStream("/message/CommonBundle$bundleFilePostfix.properties")
      ?.use { input -> properties.load(input) }
      ?: error("Can't find message bundle with postfix: $bundleFilePostfix")
    return properties.getProperty(key) ?: error("Property `$key` not found")
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
