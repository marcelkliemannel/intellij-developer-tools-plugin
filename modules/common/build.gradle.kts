plugins {
  `java-test-fixtures`
  id("org.jetbrains.intellij.platform.module")
}

dependencies {
  api(libs.bundles.text.case.converter)
  api(libs.okhttp)

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)

  testFixturesApi(libs.asm)
  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(libs.bundles.junit.implementation)
}
