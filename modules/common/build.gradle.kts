import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
  `java-test-fixtures`
}

dependencies {
  api(libs.bundles.text.case.converter)
  api(libs.okhttp)

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)

  intellijPlatform { testBundledPlugins("org.jetbrains.kotlin") }
  configurations.testFixturesApi.extendsFrom(configurations.intellijPlatformTestBundledPlugins)

  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(libs.bundles.junit.implementation)
}

val generatePluginProperties by
  tasks.registering {
    inputs.property("pluginId", project.property("pluginId"))
    inputs.property("pluginVersion", project.property("pluginVersion"))

    val outputDir = layout.buildDirectory.dir("generated-resources")
    outputs.dir(outputDir)

    doLast {
      val file = outputDir.get().file("plugin.properties").asFile
      file.parentFile.mkdirs()
      file.writeText(
        """
            pluginId=${project.property("pluginId")}
            pluginVersion=${project.property("pluginVersion")}
            pluginName=${project.property("pluginName")}
            """
          .trimIndent()
      )
    }
  }

tasks.named<Copy>("processResources") { from(generatePluginProperties.map { it.outputs.files }) }
