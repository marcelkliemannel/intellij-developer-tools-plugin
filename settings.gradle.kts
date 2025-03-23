import java.nio.file.Paths

rootProject.name = "intellij-developer-tools-plugin"

val platform = settings.extra["platform"]

val modules = mutableSetOf(
  Module("common"),
  Module("settings"),
  Module("tools-editor", Paths.get("modules/tools/editor")),
  Module("tools-ui", Paths.get("modules/tools/ui"))
)
if (platform == "IC") {
  modules.add(Module("java-dependent"))
  modules.add(Module("kotlin-dependent"))
}
modules.forEach { module ->
  include(module.name)
  project(":${module.name}").projectDir = file(module.directory)
}

data class Module(
  val name: String,
  val directory: java.nio.file.Path = Paths.get("modules/$name")
)

pluginManagement {
  repositories {
    gradlePluginPortal()
  }
}
