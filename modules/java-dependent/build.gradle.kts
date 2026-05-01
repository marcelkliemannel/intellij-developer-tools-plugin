plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  id("org.jetbrains.intellij.platform.module")
  alias(libs.plugins.spotless)
}

repositories {
  mavenLocal()
  mavenCentral()

      intellijPlatform {
        defaultRepositories()
      }
}

dependencies {
  intellijPlatform {
    bundledPlugins("com.intellij.java")
  }

  implementation(project(":common"))
  implementation(project(":tools-editor"))

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)
  testImplementation(testFixtures(project(":common")))
}
