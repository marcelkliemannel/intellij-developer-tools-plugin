package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginInfo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsInstanceSettingsLegacy.RenamedEditorPropertiesFrom630To640.renamedEditorIds
import java.util.regex.Pattern

object DeveloperToolsInstanceSettingsLegacy {
  // -- Properties ---------------------------------------------------------- //

  private val pluginVersion640 = PluginInfo.PluginVersion(6, 4, 0)

  private val configurationPropertyKeyLegacies: List<ConfigurationPropertyKeyLegacy> =
    listOf(RenamedPropertiesFrom630To640, RenamedEditorPropertiesFrom630To640)

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun applyConfigurationPropertyKeyLegacies(
    statePluginVersion: PluginInfo.PluginVersion?,
    developerToolId: String,
    propertyKey: String,
  ): String =
    configurationPropertyKeyLegacies
      .filter { it.shouldBeApplied(statePluginVersion) }
      .fold(propertyKey) { modifiedPropertyKey, legacy ->
        legacy.applyLegacy(developerToolId, modifiedPropertyKey)
      }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private interface ConfigurationPropertyKeyLegacy {

    fun shouldBeApplied(statePluginVersion: PluginInfo.PluginVersion?): Boolean

    fun applyLegacy(developerToolId: String, propertyKey: String): String
  }

  // -- Inner Type ---------------------------------------------------------- //

  private object RenamedPropertiesFrom630To640 : ConfigurationPropertyKeyLegacy {

    val migratedTextTransformersToUndirectionalConverter =
      setOf(
        "hmac-transformer",
        "text-filter",
        "text-case-transformer",
        "sql-formatting",
        "hashing-transformer",
        "text-sorting-transformer",
      )

    override fun shouldBeApplied(statePluginVersion: PluginInfo.PluginVersion?): Boolean =
      statePluginVersion == null || statePluginVersion < pluginVersion640

    /**
     * Rename property `liveTransformation` to `liveConversion` of `TextTransformer`s that have been
     * migrated to `UndirectionalConverter`.
     */
    override fun applyLegacy(developerToolId: String, propertyKey: String): String =
      if (
        propertyKey == "liveTransformation" &&
          migratedTextTransformersToUndirectionalConverter.contains(developerToolId)
      ) {
        "liveConversion"
      } else {
        propertyKey
      }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private object RenamedEditorPropertiesFrom630To640 : ConfigurationPropertyKeyLegacy {
    val sourceInputToSource = "source-input" to "source"
    val resultOutputToTarget = "source-input" to "target"
    val renamedEditorIds =
      mapOf(
        "hmac-transformer" to mapOf(sourceInputToSource, resultOutputToTarget),
        "text-filter" to mapOf(sourceInputToSource, resultOutputToTarget),
        "text-case-transformer" to mapOf(sourceInputToSource, resultOutputToTarget),
        "sql-formatting" to mapOf(sourceInputToSource, resultOutputToTarget),
        "hashing-transformer" to mapOf(sourceInputToSource, resultOutputToTarget),
        "text-sorting-transformer" to mapOf(sourceInputToSource, resultOutputToTarget),
        "jwt-encoder-decoder" to
          mapOf(
            "jwt-encoder-decoder-encoded" to "encoded",
            "jwt-encoder-decoder-header" to "header",
            "jwt-encoder-decoder-payload" to "payload",
          ),
      )
    val editorPropertySuffixes = listOf("softWraps", "showSpecialCharacters", "showWhitespaces")

    override fun shouldBeApplied(statePluginVersion: PluginInfo.PluginVersion?): Boolean =
      statePluginVersion == null || statePluginVersion < pluginVersion640

    /**
     * - Append infix `-editor-`
     * - Replace renamed editor IDs [renamedEditorIds]
     */
    override fun applyLegacy(developerToolId: String, propertyKey: String): String {
      val editorPropertySuffix = editorPropertySuffixes.firstOrNull { propertyKey.endsWith("-$it") }
      if (editorPropertySuffix == null) {
        return propertyKey
      }

      val matcher =
        Pattern.compile("^$developerToolId-(?<editorId>.+)-$editorPropertySuffix$")
          .matcher(propertyKey)
      return if (matcher.matches()) {
        var editorId = matcher.group("editorId")
        editorId = renamedEditorIds[developerToolId]?.get(editorId) ?: editorId
        "${editorId}-editor-$editorPropertySuffix"
      } else {
        propertyKey
      }
    }
  }
}
