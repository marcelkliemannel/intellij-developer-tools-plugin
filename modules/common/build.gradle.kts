import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  id("org.jetbrains.intellij.platform.module")
  alias(libs.plugins.spotless)
  `java-test-fixtures`
}

dependencies {
  api(libs.bundles.text.case.converter)
  api(libs.okhttp)

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)

  if (project.property("platform") == "idea") {
    intellijPlatform { testBundledPlugins("org.jetbrains.kotlin") }
    configurations.testFixturesApi.extendsFrom(configurations.intellijPlatformTestBundledPlugins)
  }

  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(libs.bundles.junit.implementation)
}

val generatePluginProperties by
  tasks.registering {
    val pluginId = providers.gradleProperty("pluginId")
    val pluginVersion = providers.gradleProperty("pluginVersion")
    val pluginName = providers.gradleProperty("pluginName")

    inputs.property("pluginId", pluginId)
    inputs.property("pluginVersion", pluginVersion)
    inputs.property("pluginName", pluginName)

    val outputDir = layout.buildDirectory.dir("generated-resources")
    outputs.dir(outputDir)

    doLast {
      val file = outputDir.get().file("plugin.properties").asFile
      file.parentFile.mkdirs()
      file.writeText(
        """
            pluginId=${pluginId.get()}
            pluginVersion=${pluginVersion.get()}
            pluginName=${pluginName.get()}
            """
          .trimIndent()
      )
    }
  }

tasks.named<Copy>("processResources") { from(generatePluginProperties.map { it.outputs.files }) }
