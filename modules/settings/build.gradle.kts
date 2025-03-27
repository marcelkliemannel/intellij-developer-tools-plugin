apply(plugin = "org.jetbrains.intellij.platform.module")

dependencies {
  implementation(project(":common"))

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)
  testImplementation(testFixtures(project(":common")))
}
