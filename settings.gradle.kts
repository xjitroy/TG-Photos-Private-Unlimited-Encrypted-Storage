pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "TGPhotos"
include(":app", ":core:model", ":core:designsystem", ":core:telegram", ":core:crypto",
    ":core:backup", ":core:database", ":core:media", ":core:cache", ":core:testing",
    ":feature:auth", ":feature:photos", ":feature:albums", ":feature:files",
    ":feature:vault", ":feature:settings")
