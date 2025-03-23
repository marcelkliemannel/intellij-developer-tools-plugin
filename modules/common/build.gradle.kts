plugins {
  `java-test-fixtures`
  id("org.jetbrains.intellij.platform.module")
}

dependencies {
  implementation(libs.bundles.text.case.converter)
  implementation(libs.okhttp)

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)

  testFixturesApi(libs.asm)
  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(libs.bundles.junit.implementation)
}