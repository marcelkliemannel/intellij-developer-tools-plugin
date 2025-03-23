apply(plugin = "org.jetbrains.intellij.platform.module")

dependencies {
  implementation(project(":common"))

  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.10")

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)
  testImplementation(testFixtures(project(":common")))
}
