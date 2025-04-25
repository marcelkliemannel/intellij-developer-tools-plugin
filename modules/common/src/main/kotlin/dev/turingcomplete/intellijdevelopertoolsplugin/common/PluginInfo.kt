package dev.turingcomplete.intellijdevelopertoolsplugin.common

import dev.turingcomplete.intellijdevelopertoolsplugin.common.PluginInfo.PluginVersion.Companion.toPluginVersion
import java.util.Properties

object PluginInfo {
  // -- Properties ---------------------------------------------------------- //

  private val pluginProperties: Properties by lazy {
    PluginInfo::class.java.classLoader.getResourceAsStream("plugin.properties")!!.readProperties()
  }

  val pluginId: String by lazy { pluginProperties.getProperty("pluginId") }
  val pluginVersion: PluginVersion by lazy {
    pluginProperties.getProperty("pluginVersion").toPluginVersion()
  }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  data class PluginVersion(val major: Int, val minor: Int, val patch: Int) :
    Comparable<PluginVersion> {

    override fun compareTo(other: PluginVersion): Int =
      compareValuesBy(this, other, PluginVersion::major, PluginVersion::minor, PluginVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {

      fun String.toPluginVersion(): PluginVersion {
        return this.split('.').let { (major, minor, patch) ->
          PluginVersion(major.toInt(), minor.toInt(), patch.toInt())
        }
      }
    }
  }
}
