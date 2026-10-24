plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    compileOnly(libs.packetevents.spigot)
    testImplementation(libs.packetevents.spigot)
    testImplementation(testFixtures(project(":common")))
}
