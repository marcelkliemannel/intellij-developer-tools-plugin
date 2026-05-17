package dev.turingcomplete.intellijdevelopertoolsplugin.plugin

import com.intellij.openapi.util.JDOMUtil
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.nameWithoutExtension
import org.assertj.core.api.Assertions.assertThat
import org.jdom.Element
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class PluginBundleMessagesTest {
  // -- Properties ---------------------------------------------------------- //

  // -- Initialization ------------------------------------------------------ //

  // -- Exported Methods ---------------------------------------------------- //

  @TestFactory
  fun `test that all additional languages contain the same message keys and parameter counts`():
    List<DynamicNode> =
    pluginBundles.map { pluginBundle ->
      val additionalLanguageToMessages = pluginBundle.languageToMessages - REFERENCE_LANGUAGE_KEY

      dynamicContainer(
        pluginBundle.bundleName,
        additionalLanguageToMessages.map { (additionalLanguageKey, additionalLanguageMessages) ->
          val sameMessageKeys =
            dynamicTest("Same message keys") {
              assertThat(pluginBundle.referenceLanguageMessages().keys)
                .containsExactlyInAnyOrderElementsOf(additionalLanguageMessages.keys)
            }
          val sameParameterCounts =
            dynamicContainer(
              "Language messages have same parameter counts",
              additionalLanguageMessages.map { (key, message) ->
                dynamicTest(key) {
                  assertThat(countUniqueParameters(message))
                    .isEqualTo(
                      countUniqueParameters(pluginBundle.referenceLanguageMessages()[key]!!)
                    )
                }
              },
            )

          dynamicContainer(additionalLanguageKey, listOf(sameMessageKeys, sameParameterCounts))
        },
      )
    }

  @TestFactory
  fun `test that all plugin xml message bundle references exist`(): List<DynamicNode> =
    collectPluginXmlMessageBundleUsages()
      .groupBy { it.bundleName }
      .map { (bundleName, messageBundleUsages) ->
        dynamicContainer(
          bundleName,
          messageBundleUsages.map { messageBundleUsage ->
            dynamicTest(messageBundleUsage.displayableText) {
              assertThat(getPluginBundle(bundleName).referenceLanguageMessages())
                .containsKey(messageBundleUsage.messageKey)
            }
          },
        )
      }

  @TestFactory
  fun `test that all keys in the plugin bundle are used in plugin xml`(): List<DynamicNode> {
    val usedMessageKeysByBundleName =
      collectPluginXmlMessageBundleUsages()
        .groupBy { it.bundleName }
        .mapValues { (_, usages) -> usages.map { it.messageKey }.toSet() }

    return pluginBundles.map { pluginBundle ->
      dynamicContainer(
        pluginBundle.bundleName,
        pluginBundle.referenceLanguageMessages().keys.map { messageKey ->
          dynamicTest(messageKey) {
            assertThat(usedMessageKeysByBundleName[pluginBundle.bundleName])
              .describedAs("No unused plugin bundle keys")
              .contains(messageKey)
          }
        },
      )
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun collectPluginXmlMessageBundleUsages(): List<PluginBundleUsage> {
    val defaultPluginBundleNames = pluginXml.getElementValuesRecursively("resource-bundle")
    assertThat(defaultPluginBundleNames).contains(PLUGIN_BUNDLE_NAME)

    return collectDefaultPluginXmlMessageBundleUsages(defaultPluginBundleNames) +
      collectExplicitPluginXmlMessageBundleUsages()
  }

  private fun collectDefaultPluginXmlMessageBundleUsages(
    defaultPluginBundleNames: List<String>
  ): List<PluginBundleUsage> =
    pluginXml
      .collectStringValuesRecursively()
      .flatMap { stringValue ->
        TRANSLATABLE_VALUE_REGEX.findAll(stringValue).map { matchResult ->
          defaultPluginBundleNames.map { bundleName ->
            PluginBundleUsage(
              bundleName = bundleName,
              messageKey = matchResult.groupValues[1],
              displayableText = stringValue,
            )
          }
        }
      }
      .flatten()

  private fun collectExplicitPluginXmlMessageBundleUsages(): List<PluginBundleUsage> =
    pluginXml
      .collectElementsRecursively()
      .mapNotNull { element ->
        val bundleName = element.getAttributeValue("bundle") ?: return@mapNotNull null
        val messageKey = element.getAttributeValue("key") ?: return@mapNotNull null

        PluginBundleUsage(
          bundleName = bundleName,
          messageKey = messageKey,
          displayableText = "${element.name}: $bundleName/$messageKey",
        )
      }
      .filter { it.bundleName == PLUGIN_BUNDLE_NAME }

  private fun Element.collectStringValuesRecursively(): List<String> =
    attributes.map { it.value } +
      listOfNotNull(textTrim.takeIf { it.isNotBlank() }) +
      children.flatMap { it.collectStringValuesRecursively() }

  private fun Element.collectElementsRecursively(): List<Element> =
    listOf(this) + children.flatMap { it.collectElementsRecursively() }

  private fun Element.getElementValuesRecursively(vararg elementNames: String): List<String> {
    val values = mutableListOf<String>()

    if (this.name in elementNames) {
      this.value?.let { values.add(it) }
    }

    this.children.forEach { child ->
      values.addAll(child.getElementValuesRecursively(*elementNames))
    }

    return values
  }

  private fun getPluginBundle(bundleName: String): PluginBundle =
    pluginBundles.firstOrNull { it.bundleName == bundleName }
      ?: error("Bundle `$bundleName` not found")

  private fun countUniqueParameters(message: String): Int =
    Regex("(?<!\\$)\\{(\\d+)}").findAll(message).map { it.groupValues[1].toInt() }.toSet().size

  // -- Inner Type ---------------------------------------------------------- //

  private data class PluginBundle(
    val bundleName: String,
    val languageToMessages: MutableMap<String, Map<String, String>> = mutableMapOf(),
  ) {

    fun referenceLanguageMessages(): Map<String, String> =
      languageToMessages[REFERENCE_LANGUAGE_KEY]
        ?: error("Bundle `$bundleName` is missing the reference language")
  }

  private data class PluginBundleUsage(
    val bundleName: String,
    val messageKey: String,
    val displayableText: String,
  )

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private lateinit var pluginXml: Element

    private lateinit var pluginBundles: List<PluginBundle>

    private const val PLUGIN_BUNDLE_NAME = "messages.PluginBundle"
    private const val REFERENCE_LANGUAGE_KEY = "en"

    private val TRANSLATABLE_VALUE_REGEX = Regex("^%([A-Za-z0-9_.-]+)$")

    @BeforeAll
    @JvmStatic
    fun beforeAll() {
      Files.newInputStream(Path.of("src/main/resources/META-INF/plugin.xml")).use {
        pluginXml = JDOMUtil.load(it)
      }

      pluginBundles = collectPluginBundles(Path.of("src/main/resources/messages"))
      assertThat(pluginBundles).extracting<String> { it.bundleName }.contains(PLUGIN_BUNDLE_NAME)
    }

    private fun collectPluginBundles(messagesBundlesDir: Path): List<PluginBundle> {
      check(Files.isDirectory(messagesBundlesDir))

      val result = mutableMapOf<String, PluginBundle>()

      Files.list(messagesBundlesDir).use { files ->
        files
          .filter { it.fileName.toString().endsWith(".properties") }
          .forEach { file ->
            val filename = file.nameWithoutExtension
            val parts = filename.split("_")

            val bundleName = "messages.${if (parts.size > 1) parts[0] else filename}"
            val languageKey = if (parts.size > 1) parts[1] else REFERENCE_LANGUAGE_KEY
            val properties = Properties().apply { Files.newInputStream(file).use { load(it) } }

            val pluginBundle = result.getOrPut(bundleName) { PluginBundle(bundleName) }
            pluginBundle.languageToMessages[languageKey] =
              properties.stringPropertyNames().associateWith { properties.getProperty(it) }
          }
      }

      return result.values.toList()
    }
  }
}
