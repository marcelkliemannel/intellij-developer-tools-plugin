dependencies {
  intellijPlatform {
    bundledPlugins("org.jetbrains.kotlin")
  }
}

dependencies {
  implementation(project(":common"))

  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)
  testImplementation(testFixtures(project(":common")))
}