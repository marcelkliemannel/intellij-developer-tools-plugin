package dev.turingcomplete.intellijdevelopertoolsplugins.developertool._internal

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService.DeveloperToolConfigurationProperty
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.xmlunit.assertj.XmlAssert
import org.xmlunit.diff.DefaultNodeMatcher
import org.xmlunit.diff.ElementSelectors
import java.io.StringWriter

class DeveloperToolsPluginServiceTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Test
  fun `test full State serialization and deserialization`() {
    val expectedXml = DeveloperToolsPluginServiceTest::class.java.getResource("/dev/turingcomplete/intellijdeveloperplugin/developer-tools.xml")!!
    val stateFromExpectedXml = XmlSerializer.deserialize(expectedXml, DeveloperToolsPluginService.State::class.java)

    val developerToolsPluginService = DeveloperToolsPluginService().apply { loadState(stateFromExpectedXml) }
    val actualState = XmlSerializer.serialize(developerToolsPluginService.state)
    val actualXml = StringWriter().apply { JDOMUtil.write(actualState, this, System.lineSeparator()) }.toString()

    XmlAssert.assertThat(expectedXml).and(actualXml)
            .ignoreWhitespace()
            .withNodeMatcher(DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
            .areSimilar()
  }

  @Test
  fun `test 'DeveloperToolConfigurationValueConverter'`() {
    listOf<Any>(true, 1, 1L, 1f, 1.0, "String", CharCategory.CURRENCY_SYMBOL).forEach { originalValue ->
      val originalProperty = DeveloperToolConfigurationProperty("abc", "enum", originalValue)
      val originalState = DeveloperToolsPluginService.State()
      originalState.developerToolsConfigurationProperties = listOf(originalProperty)
      val serializedState = XmlSerializer.serialize(originalState)
      val serializedXml = StringWriter().apply { JDOMUtil.write(serializedState, this, System.lineSeparator()) }.toString()

      val deserializedState = XmlSerializer.deserialize(JDOMUtil.load(serializedXml), DeveloperToolsPluginService.State::class.java)
      val restoredProperty = deserializedState.developerToolsConfigurationProperties!![0]
      val restoredValue = restoredProperty.value

      assertThat(originalValue).isEqualTo(restoredValue)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}