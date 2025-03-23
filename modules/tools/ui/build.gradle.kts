apply(plugin = "org.jetbrains.intellij.platform.module")

dependencies {
  implementation(project(":common"))
  implementation(project(":settings"))

  implementation(libs.commons.text)
  implementation(libs.commons.codec)
  implementation(libs.commons.io)
  implementation(libs.commons.compress)
  implementation(libs.jose4j) {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  implementation(libs.bundles.jackson)
  implementation(libs.named.regexp)
  implementation(libs.bundles.zxing)
  implementation(libs.sql.formatter)
  implementation(libs.csscolor4j)
  implementation(libs.jfiglet)
  implementation(libs.jsonpath) {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  implementation(libs.json.schema.validator) {
    exclude("org.apache.commons", "commons-lang3")
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
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
