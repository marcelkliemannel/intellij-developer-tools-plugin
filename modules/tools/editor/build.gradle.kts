dependencies {
  implementation(project(":common"))
  // This is required for the `OpenDeveloperToolService` mechanism. However, a
  // better solution would be to have a common module that utilise the message
  // bus to open ui tools from other modules.
  implementation(project(":tools-ui"))

  implementation(libs.commons.text)
  implementation(libs.bundles.text.case.converter)
  implementation(libs.ulid.creator)
  implementation(libs.jnanoid)
  implementation(libs.uuid.generator) {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)
  testImplementation(testFixtures(project(":common")))
}
