import java.nio.file.Paths
import java.util.*
import kotlin.io.path.inputStream

object PluginUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun getPluginVersion(): String =
    Properties().apply { load(Paths.get("gradle.properties").inputStream()) }["pluginVersion"] as String

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}