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
