plugins {
    base
    id("com.gradleup.shadow") version "8.3.6" apply false
}

allprojects {
    group = "ru.gloom"
    version = "1.0.1"
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    }

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
        options.compilerArgs.add("-Xlint:deprecation")
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.10.2")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

tasks.register("test") {
    dependsOn(subprojects.map { "${it.path}:test" })
}

tasks.register("shadowJar") {
    dependsOn(":plugin:shadowJar")
}

tasks.named("build") {
    dependsOn("test", "shadowJar")
}

tasks.named("clean") {
    dependsOn(subprojects.map { "${it.path}:clean" })
}
