dependencies {
  intellijPlatform {
    bundledPlugins("com.intellij.java")
  }

  implementation(project(":common"))

  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)
  testImplementation(testFixtures(project(":common")))
}