plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  id("org.jetbrains.intellij.platform.module")
  alias(libs.plugins.spotless)
}

dependencies {
  implementation(project(":common"))

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)
  testImplementation(testFixtures(project(":common")))
}
