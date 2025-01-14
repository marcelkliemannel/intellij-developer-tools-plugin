rootProject.name = "intellij-developer-tools-plugin"

val platform = settings.extra["platform"]

val modules = mutableListOf("common")
if (platform == "IC") {
  modules.add("java-dependent")
  modules.add("kotlin-dependent")
}
modules.forEach { projectName ->
  include(projectName)
  project(":$projectName").projectDir = file("modules/$projectName")
}
