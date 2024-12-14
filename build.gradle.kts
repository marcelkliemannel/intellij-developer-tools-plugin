
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  java
  // See bundled version: https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
  kotlin("jvm") version "2.0.21"
  id("org.jetbrains.intellij.platform") version "2.2.1"
  id("org.jetbrains.changelog") version "2.2.0"
  id("com.autonomousapps.dependency-analysis") version "1.30.0"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
  mavenLocal()

  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    val platformVersion = properties("platformVersion")
    create(properties("platform"), platformVersion, false)

    bundledPlugins(properties("platformBundledPlugins").split(','))

    instrumentationTools()
    pluginVerifier()
    zipSigner()

    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)
  }

  implementation("com.fasterxml.uuid:java-uuid-generator:5.0.0") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  implementation("com.jayway.jsonpath:json-path:2.9.0") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  val jacksonVersion = "2.17.0"
  implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:$jacksonVersion")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:$jacksonVersion")
  implementation("com.github.tony19:named-regexp:1.0.0")
  implementation("org.apache.commons:commons-text:1.10.0")
  implementation("commons-codec:commons-codec:1.15")
  val textCaseConverterVersion = "2.0.0"
  implementation("dev.turingcomplete:text-case-converter:$textCaseConverterVersion")
  implementation("dev.turingcomplete:text-case-converter-kotlin-extension:$textCaseConverterVersion")
  implementation("com.github.vertical-blank:sql-formatter:2.0.4")
  implementation(kotlin("stdlib"))
  implementation("com.networknt:json-schema-validator:1.4.0") {
    exclude("org.apache.commons", "commons-lang3")
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  implementation("org.apache.commons:commons-compress:1.26.0")
  val zxing = "3.5.3"
  implementation("com.google.zxing:core:$zxing")
  implementation("com.google.zxing:javase:$zxing")
  implementation("com.aventrix.jnanoid:jnanoid:2.0.0")
  implementation("com.github.f4b6a3:ulid-creator:5.2.3")
  implementation("org.silentsoft:csscolor4j:1.0.0")
  implementation("commons-io:commons-io:2.15.1")
  implementation("org.bitbucket.b_c:jose4j:0.9.6") {
    exclude(group = "org.slf4j", module = "slf4j-api")
  }

  testImplementation("org.assertj:assertj-core:3.25.3")
  val junitVersion = "5.10.2"
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
  // This is a workaround, see: https://youtrack.jetbrains.com/issue/IJPL-159134/JUnit5-Test-Framework-refers-to-JUnit4-java.lang.NoClassDefFoundError-junit-framework-TestCase
  testRuntimeOnly("junit:junit:4.13.2")
}

intellijPlatform {
  pluginConfiguration {
    version = providers.gradleProperty("pluginVersion")
    ideaVersion {
      sinceBuild = properties("pluginSinceBuild")
      untilBuild = provider { null }
    }
    changeNotes.set(provider { changelog.renderItem(changelog.get(project.version as String), Changelog.OutputType.HTML) })
  }

  signing {
    val jetbrainsDir = File(System.getProperty("user.home"), ".jetbrains")
    certificateChain.set(project.provider { File(jetbrainsDir, "plugin-sign-chain.crt").readText() })
    privateKey.set(project.provider { File(jetbrainsDir, "plugin-sign-private-key.pem").readText() })
    password.set(project.provider { properties("jetbrains.sign-plugin.password") })
  }

  publishing {
    //        dependsOn("patchChangelog")
    token.set(project.provider { properties("jetbrains.marketplace.token") })
    channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
  }

  pluginVerification {
    failureLevel.set(
      listOf(
        COMPATIBILITY_PROBLEMS, INTERNAL_API_USAGES, NON_EXTENDABLE_API_USAGES,
        OVERRIDE_ONLY_API_USAGES, MISSING_DEPENDENCIES, INVALID_PLUGIN
      )
    )

    ides {
      recommended()
    }
  }
}

changelog {
  val projectVersion = project.version as String
  version.set(projectVersion)
  header.set("[$projectVersion] - ${org.jetbrains.changelog.date()}")
  groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
}

val writeChangelogToFileTask = tasks.create("writeChangelogToFile") {
  outputs.dir("${buildDir}/generated-resources/changelog")

  doLast {
    val renderResult = changelog.instance.get().releasedItems.joinToString("\n") { changelog.renderItem(it, Changelog.OutputType.HTML) }
    val baseDir = "${buildDir}/generated-resources/changelog/dev/turingcomplete/intellijdevelopertoolsplugin"
    file(baseDir).mkdirs()
    file("${baseDir}/changelog.html").writeText(renderResult)
  }
}

sourceSets {
  main {
    resources {
      srcDir(writeChangelogToFileTask)
    }
  }
}

dependencyAnalysis {
  issues {
    all {
      onUsedTransitiveDependencies {
        severity("fail")
      }
      onUnusedDependencies {
        severity("fail")
      }
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget.set(JvmTarget.JVM_21)
    }
  }

  named("buildSearchableOptions") {
    enabled = false
  }

  withType<Test> {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "false")
  }

  named("check") {
    dependsOn("buildHealth")
  }

  named<RunIdeTask>("runIde") {
    jvmArgumentProviders += CommandLineArgumentProvider {
      // https://kotlin.github.io/analysis-api/testing-in-k2-locally.html
      listOf("-Didea.kotlin.plugin.use.k2=false")
    }
  }
}