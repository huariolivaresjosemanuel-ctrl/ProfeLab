pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "ProfeLab"
include(":app")
include(":core:model")
include(":core:database")
include(":core:ui")
include(":core:files")
include(":core:content")
include(":core:security")
include(":feature:home")
include(":feature:taller")
include(":feature:sessions")
include(":feature:notebook")
include(":feature:library")
include(":feature:pdf")
include(":feature:capture")
include(":feature:ocr")
include(":feature:profile")
