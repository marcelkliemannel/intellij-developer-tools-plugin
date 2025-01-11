plugins {
  `java-test-fixtures`
}

dependencies {
  implementation(libs.commons.text)
  implementation(libs.bundles.text.case.converter)

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testImplementation(libs.bundles.junit.runtime)

  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(libs.bundles.junit.implementation)
}