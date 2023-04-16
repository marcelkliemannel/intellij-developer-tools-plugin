import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  java
  kotlin("jvm") version "1.7.10"
  id("org.jetbrains.intellij") version "1.13.0"
  id("org.jetbrains.changelog") version "2.0.0"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  val bouncycastleVersion = 1.72
  implementation("org.bouncycastle:bcprov-jdk18on:$bouncycastleVersion")
  implementation("org.bouncycastle:bcpkix-jdk18on:$bouncycastleVersion")
  implementation("com.fasterxml.uuid:java-uuid-generator:4.1.0") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  implementation("com.jayway.jsonpath:json-path:2.7.0") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  implementation("com.auth0:java-jwt:4.3.0")
  val jacksonVersion = "2.14.2"
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:$jacksonVersion")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:$jacksonVersion")
  implementation("com.github.tony19:named-regexp:0.2.8")
  implementation("org.apache.commons:commons-text:1.10.0")
  val textCaseConverterVersion = "1.0.0"
  implementation("dev.turingcomplete:text-case-converter:$textCaseConverterVersion")
  implementation("dev.turingcomplete:text-case-converter-kotlin-extension:$textCaseConverterVersion")
  implementation("com.github.vertical-blank:sql-formatter:2.0.3")
  implementation(kotlin("stdlib"))
  implementation("com.networknt:json-schema-validator:1.0.79") {
    exclude("org.apache.commons", "commons-lang3")
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  val zxing = "3.5.1"
  implementation("com.google.zxing:core:$zxing")
  implementation("com.google.zxing:javase:$zxing")

  testImplementation("org.assertj:assertj-core:3.24.2")
  testImplementation("org.xmlunit:xmlunit-assertj:2.9.1")
  testImplementation("org.skyscreamer:jsonassert:1.5.0")
}

intellij {
  version.set(properties("platformVersion"))
  type.set(properties("platformType"))
  downloadSources.set(properties("platformDownloadSources").toBoolean())
  updateSinceUntilBuild.set(true)
  plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

changelog {
  val projectVersion = project.version as String
  version.set(projectVersion)
  header.set("[$projectVersion] - ${org.jetbrains.changelog.date()}")
  groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
}

tasks {
  patchPluginXml {
    version.set(properties("pluginVersion"))
    sinceBuild.set(properties("pluginSinceBuild"))
    untilBuild.set(properties("pluginUntilBuild"))
    // TODO: REMOVE
    // changeNotes.set(provider { changelog.renderItem(changelog.getLatest(), HTML) })
  }

  runPluginVerifier {
    ideVersions.set(properties("pluginVerifierIdeVersions").split(",").map(String::trim).filter(String::isNotEmpty))
  }

  publishPlugin {
    dependsOn("patchChangelog")
    token.set(project.provider { properties("jetbrains.marketplace.token") })
    channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
  }

  signPlugin {
    val jetbrainsDir = File(System.getProperty("user.home"), ".jetbrains")
    certificateChain.set(project.provider { File(jetbrainsDir, "plugin-sign-chain.crt").readText() })
    privateKey.set(project.provider { File(jetbrainsDir, "plugin-sign-private-key.pem").readText() })

    password.set(project.provider { properties("jetbrains.sign-plugin.password") })
  }

  withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "17"
    }
  }

  withType<Test> {
    systemProperty("idea.test.execution.policy", "dev.turingcomplete.intellijdevelopertoolsplugins.developertool._internal.tool.DeveloperToolTestPolicy")
  }
}