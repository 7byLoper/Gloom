rootProject.name = "Gloom"
include("common", "plugin", "compatibility:legacy", "compatibility:display")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}
