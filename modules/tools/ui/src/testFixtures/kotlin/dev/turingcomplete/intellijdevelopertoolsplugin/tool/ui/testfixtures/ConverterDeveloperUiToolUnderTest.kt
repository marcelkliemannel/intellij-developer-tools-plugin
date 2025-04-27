package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.testfixtures

import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactoryEp
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.Converter

class ConverterDeveloperUiToolUnderTest(
  factoryEp: DeveloperUiToolFactoryEp<out DeveloperUiToolFactory<*>>,
  configuration: DeveloperToolConfiguration,
  instance: Converter,
) :
  DeveloperUiToolUnderTest<Converter>(
    factoryEp = factoryEp,
    configuration = configuration,
    instance = instance,
  ) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun randomisePropertyValue(property: DeveloperToolConfiguration.PropertyContainer): Any =
    when (property.key) {
      "sourceText" -> randomString()
      "targetText" ->
        String(instance.doConvertToTarget(getStringPropertyValue("sourceText").toByteArray()))
      else -> super.randomisePropertyValue(property)
    }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
