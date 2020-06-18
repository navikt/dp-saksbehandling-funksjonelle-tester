plugins {
    application
    kotlin("jvm") version Kotlin.version
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
    id("com.palantir.git-version") version "0.11.0"
}

buildscript {
    repositories {
        jcenter()
        maven("https://jitpack.io")
    }
}

apply {
    plugin(Spotless.spotless)
}

repositories {
    mavenCentral()
    jcenter()
    maven("http://packages.confluent.io/maven/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

val gitVersion: groovy.lang.Closure<Any> by extra
version = gitVersion()
group = "no.nav.dagpenger"

application {
    applicationName = "dp-saksbehandling-funksjonelle-tester"
    mainClassName = "no.nav.dagpenger.RunCucumberKt"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("io.cucumber:cucumber-java8:6.1.1")

    implementation("org.awaitility:awaitility-kotlin:4.0.3")

    implementation(kotlin("test"))
    implementation(KoTest.assertions)
    implementation(KoTest.runner)

    implementation(Kotlin.Logging.kotlinLogging)

    implementation(Konfig.konfig)

    implementation(RapidAndRivers)

    implementation(Ulid.ulid)

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:${Kotlin.version}")
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.kt*")
        ktlint()
    }
}
