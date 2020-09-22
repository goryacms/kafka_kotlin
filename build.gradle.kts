import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val kotlinVersion = "1.4.0"

    repositories {
        maven {
            val nexusRepo: String by project
            url = uri(nexusRepo)
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
    }
}

plugins {
    id("scala")

    application
    kotlin("jvm") version "1.4.0"
    kotlin("plugin.spring") version "1.4.0"

    id("com.github.ben-manes.versions") version "0.27.0"
    kotlin("kapt") version "1.4.0"
}

extra["springBootVersion"] = "2.2.5.RELEASE"
extra["springCloudVersion"] = "Hoxton.SR1"

group = "ru.my.sample"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    maven {
        val nexusRepo: String by project
        url = uri(nexusRepo)
    }
}

dependencies {
    // only for applying all necessary exlusions
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))
    implementation("org.springframework.boot:spring-boot-starter-actuator:${property("springBootVersion")}")
//    implementation("org.springframework.boot:spring-boot-starter-jdbc:${property("springBootVersion")}")

    implementation("io.micrometer:micrometer-registry-prometheus:1.5.2")

    // scala
    implementation("org.scala-lang:scala-library:2.12.10")
    implementation("org.typelevel:cats-core_2.12:2.0.0")
    implementation("org.typelevel:cats-effect_2.12:2.0.0")
    implementation("io.circe:circe-core_2.12:0.12.2") {
        exclude(group = "org.typelevel", module = "cats-core_2.12")
    }
    implementation("com.github.cb372:cats-retry-core_2.12:0.3.0")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.2")
    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.11.2")

    implementation("org.apache.kafka:kafka-clients:2.4.1")
    implementation("org.springframework.kafka:spring-kafka:${property("springBootVersion")}")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.9")

    implementation("io.arrow-kt:arrow-core-data:0.10.5")

    //implementation("org.liquibase:liquibase-core:3.8.6")
    implementation("com.google.guava:guava:28.2-jre")
    implementation("de.siegmar:logback-gelf:2.1.0")

    kapt("org.springframework.boot:spring-boot-configuration-processor:${property("springBootVersion")}")
}

dependencies {
    // scala dependencies
    testImplementation("org.scala-lang:scala-library:2.12.10")
    testImplementation("org.scala-lang.modules:scala-java8-compat_2.12:0.9.1")

    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("org.mockito:mockito-core:3.3.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.9")

    testImplementation("org.jeasy:easy-random-core:4.2.0")
    testImplementation("org.jeasy:easy-random-randomizers:4.2.0")

    testImplementation("org.testcontainers:testcontainers:1.13.0")
//    testImplementation("org.testcontainers:postgresql:1.13.0")
    testImplementation("org.testcontainers:kafka:1.13.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${property("springBootVersion")}") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation ("org.junit.platform:junit-platform-runner:1.4.1")
    testImplementation ("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    testImplementation("org.jetbrains.exposed:exposed:0.17.7")
    testCompile("org.testcontainers:junit-jupiter:1.11.2")
    testAnnotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${property("springBootVersion")}")
    testImplementation ("org.awaitility:awaitility:4.0.3")
}

configurations {
    all {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "org.mortbay.jetty", module = "servlet-api-2.5")
        exclude(group = "net.logstash.logback", module = "logstash-logback-encoder")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("user.timezone", "UTC")

    minHeapSize = "128m"
    maxHeapSize = "6g"

    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
        jvmTarget = "11"
    }
}

application {
    mainClassName = "sample.SampleLinuxKt"
}

tasks.register("create") {

    doLast {
        println(projectDir.path)
        val scenario = project.properties["scenario"]
            ?: ""
        if (scenario.toString().isNotEmpty()) {
            copy {
                from("${projectDir.path}/src/test/resources/bootstrap/example")
                into("${projectDir.path}/src/test/resources/scenarios/${scenario}")
            }
            println("Scenario created at ${projectDir.path}/src/test/resources/scenarios/${scenario}")
        } else {
            throw GradleException("Define scenario ./grarlew create -Pscenario=suiteName/scenarioName")
        }
    }
}