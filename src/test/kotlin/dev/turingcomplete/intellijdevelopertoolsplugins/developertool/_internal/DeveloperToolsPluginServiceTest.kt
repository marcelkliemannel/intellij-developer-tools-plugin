package dev.turingcomplete.intellijdevelopertoolsplugins.developertool._internal

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService
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
  fun `test State serialization and deserialization`() {
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

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}