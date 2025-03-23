package dev.turingcomplete.intellijdevelopertoolsplugin.plugin

import com.intellij.openapi.util.JDOMUtil
import org.assertj.core.api.Assertions.assertThat
import org.jdom.Element
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PluginXmlTest {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @Test
  fun `test that all referenced class name exist`() {
    val referencedClassNamesInAttributes = pluginXml.getAttributeValuesRecursively("implementation", "class", "implementationClass", "implements", "beanClass", "instance", "factoryClass")
    val referencedClassNamesInValues = pluginXml.getElementValuesRecursively("className").map { it.trim() }
    val referencedClassNames = referencedClassNamesInAttributes
      .plus(referencedClassNamesInValues)
      .map { it.replace("&", ".") }
    assertThat(referencedClassNames).hasSizeGreaterThan(25)

    val missingReferencedClassNames = referencedClassNames.filter {
      try {
        println("Loading class: $it")
        Class.forName(it)
        return@filter false
      }
      catch (_: Exception) {
        return@filter true
      }
    }

    assertThat(missingReferencedClassNames).describedAs("No missing referenced class names").isEmpty()
  }

  @Test
  fun `test that all referenced files exist`() {
    val referencedFiles = pluginXml.getAttributeValuesRecursively("config-file", "icon")
      .map { if (it.startsWith("/")) it else "/META-INF/$it" }
    assertThat(referencedFiles).hasSizeGreaterThan(2)

    val missingReferencedFiles = referencedFiles.filter {
      println("Loading file: $it")
      PluginXmlTest::class.java.getResource(it) == null
    }

    assertThat(missingReferencedFiles).describedAs("No missing referenced files").isEmpty()
  }

  // -- Private Methods ----------------------------------------------------- //

  fun Element.getAttributeValuesRecursively(vararg attributeNames: String): List<String> {
    val values = mutableListOf<String>()

    attributeNames.forEach { attr ->
      this.getAttributeValue(attr)?.let { values.add(it) }
    }

    this.children.forEach { child ->
      values.addAll(child.getAttributeValuesRecursively(*attributeNames))
    }

    return values
  }

  fun Element.getElementValuesRecursively(vararg elementNames: String): List<String> {
    val values = mutableListOf<String>()

    if (this.name in elementNames) {
      this.value?.let { values.add(it) }
    }

    this.children.forEach { child ->
      values.addAll(child.getElementValuesRecursively(*elementNames))
    }

    return values
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    lateinit var pluginXml: Element

    @BeforeAll
    @JvmStatic
    fun beforeAll() {
      pluginXml = JDOMUtil.load(PluginXmlTest::class.java.getResourceAsStream("/META-INF/plugin.xml"))
    }
  }
}
