pluginManagement {
    repositories {
        // Kotlin DSL plugin transitive dependencies are published to Maven Central.
        // Keep the plugin portal for plugin marker artifacts, but search Maven repositories too.
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
