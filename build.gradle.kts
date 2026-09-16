plugins {
    `maven-publish`
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ru.gloom"
version = "1.0.3-RELEASE"

repositories {
    exclusiveContent {
        forRepository {
            maven { url = uri("https://maven.enginehub.org/repo/") }
        }
        filter {
            includeGroup("com.sk89q.worldedit")
            includeGroup("com.sk89q.worldguard")
        }
    }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://repo.viaversion.com") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://repo.opencollab.dev/main/") }
    maven { url = uri("https://repo.opencollab.dev/maven-snapshots/") }
    maven { url = uri("https://repo.opencollab.dev/maven-releases/") }
    maven { url = uri("https://maven.citizensnpcs.co/repo") }
    maven { url = uri("https://libraries.minecraft.net/") }
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnly(libs.viaversion)
    compileOnly(libs.packetevents.spigot)
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.5") {
        isTransitive = false
    }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.5") {
        isTransitive = false
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.17") {
        isTransitive = false
    }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.17") {
        isTransitive = false
    }

    compileOnly(libs.geyser.api)
    compileOnly(libs.floodgate.api)

    implementation("com.google.flatbuffers:flatbuffers-java:25.2.10")
    implementation("io.lettuce:lettuce-core:7.4.0.RELEASE")

    implementation(libs.relocations)
    compileOnly("net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }
    compileOnly(fileTree("libs") {
        include("*.jar")
    })
}

tasks {
    test {
        useJUnitPlatform()
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
    shadowJar {
        relocate("com.fasterxml.jackson", "ru.gloom.shade.jackson")
        relocate("okhttp3", "ru.gloom.shade.okhttp3")
        relocate("okio", "ru.gloom.shade.okio")
        relocate("kotlin", "ru.gloom.shade.kotlin")
        relocate("org.reflections", "ru.gloom.shade.reflections")
        relocate("javassist", "ru.gloom.shade.javassist")

        relocate("io.lettuce", "ru.gloom.shade.lettuce")
        relocate("io.netty", "ru.gloom.shade.netty")

        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/maven/**")
        exclude("META-INF/services/**")
        exclude("META-INF/versions/**")

        minimize {
            exclude(dependency("com.fasterxml.jackson.core:jackson-annotations:.*"))
            exclude(dependency("com.fasterxml.jackson.core:jackson-core:.*"))
        }

        mergeServiceFiles()

        manifest {
            attributes(
                "Implementation-Version" to project.version,
                "Main-Class" to "ru.gloom.GloomAI"
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    clean {
        delete(fileTree("build/libs") {
            include("*.jar")
        })
    }
}

java {
    val targetJavaVersion = 17
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion

    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifact(tasks.shadowJar.get())
            artifactId = project.name
            groupId = project.group.toString()
            version = project.version.toString()
        }
    }
}

artifacts {
    add("default", tasks.shadowJar)
}
