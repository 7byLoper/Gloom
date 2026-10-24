plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.packetevents.spigot)
    testImplementation(libs.packetevents.spigot)
    testFixturesImplementation(libs.packetevents.spigot)
    testFixturesImplementation("org.mockito:mockito-core:5.12.0")
}
